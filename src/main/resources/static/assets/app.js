const analyzeForm = document.getElementById("analyzeForm");
const urlInput = document.getElementById("urlInput");
const resultPanel = document.getElementById("resultPanel");
const riskBadge = document.getElementById("riskBadge");
const scoreText = document.getElementById("scoreText");
const flagsList = document.getElementById("flagsList");
const errorText = document.getElementById("errorText");
const historyBody = document.getElementById("historyBody");
const refreshHistoryBtn = document.getElementById("refreshHistory");
const clearHistoryBtn = document.getElementById("clearHistory");
const riskFilter = document.getElementById("riskFilter");
const healthStatus = document.getElementById("healthStatus");
const themeToggle = document.getElementById("themeToggle");
const navLinks = Array.from(document.querySelectorAll(".nav-link"));
const revealTargets = Array.from(document.querySelectorAll(".reveal"));
const sectionTargets = Array.from(document.querySelectorAll("main section[id]"));

const THEME_KEY = "sentinel-theme";

function currentTheme() {
  return document.documentElement.getAttribute("data-theme") || "light";
}

function applyTheme(theme) {
  document.documentElement.setAttribute("data-theme", theme);
  themeToggle.textContent = theme === "dark" ? "Light mode" : "Dark mode";
}

function initializeTheme() {
  const stored = localStorage.getItem(THEME_KEY);
  if (stored === "light" || stored === "dark") {
    applyTheme(stored);
    return;
  }

  const prefersDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  applyTheme(prefersDark ? "dark" : "light");
}

function setActiveNavByHash(hash) {
  for (const link of navLinks) {
    const isActive = link.getAttribute("href") === hash;
    link.classList.toggle("active", isActive);
  }
}

function initializeNavBehavior() {
  for (const link of navLinks) {
    link.addEventListener("click", () => {
      const href = link.getAttribute("href");
      if (href && href.startsWith("#")) {
        setActiveNavByHash(href);
      }
    });
  }

  if (!sectionTargets.length || !navLinks.length) {
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    const visible = entries
      .filter((entry) => entry.isIntersecting)
      .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

    if (!visible || !visible.target || !visible.target.id) {
      return;
    }

    setActiveNavByHash(`#${visible.target.id}`);
  }, {
    threshold: [0.3, 0.55, 0.85],
    rootMargin: "-20% 0px -35% 0px"
  });

  for (const section of sectionTargets) {
    observer.observe(section);
  }

  if (window.location.hash) {
    setActiveNavByHash(window.location.hash);
  } else {
    setActiveNavByHash("#overview");
  }
}

function initializeRevealAnimation() {
  if (!revealTargets.length) {
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        observer.unobserve(entry.target);
      }
    }
  }, {
    threshold: 0.16,
    rootMargin: "0px 0px -8% 0px"
  });

  for (const target of revealTargets) {
    observer.observe(target);
  }
}

function riskClass(level) {
  return `risk-${String(level).toLowerCase()}`;
}

function showError(message) {
  errorText.textContent = message;
  errorText.classList.remove("hidden");
}

function clearError() {
  errorText.classList.add("hidden");
  errorText.textContent = "";
}

function renderResult(data) {
  resultPanel.classList.remove("hidden");
  riskBadge.className = `risk-badge ${riskClass(data.riskLevel)}`;
  riskBadge.textContent = data.riskLevel;
  scoreText.textContent = `Score: ${data.score}`;

  flagsList.innerHTML = "";
  const flags = Array.isArray(data.flags) ? data.flags : [];
  if (!flags.length) {
    const li = document.createElement("li");
    li.textContent = "No suspicious signals detected.";
    flagsList.appendChild(li);
    return;
  }

  for (const flag of flags) {
    const li = document.createElement("li");
    li.textContent = flag;
    flagsList.appendChild(li);
  }
}

function truncate(value, max = 80) {
  if (!value) return "";
  return value.length <= max ? value : `${value.slice(0, max - 3)}...`;
}

function renderHistory(records) {
  if (!records.length) {
    historyBody.innerHTML = '<tr><td colspan="5" class="empty">No scans yet.</td></tr>';
    return;
  }

  historyBody.innerHTML = "";
  for (const record of records) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${record.scannedAt ?? ""}</td>
      <td title="${record.url}">${truncate(record.url, 74)}</td>
      <td><span class="risk-badge ${riskClass(record.riskLevel)}">${record.riskLevel}</span></td>
      <td>${record.score}</td>
      <td title="${record.flagsText ?? ""}">${truncate(record.flagsText ?? "", 72)}</td>
    `;
    historyBody.appendChild(row);
  }
}

async function checkHealth() {
  try {
    const response = await fetch("/api/health");
    if (!response.ok) throw new Error("Health endpoint unavailable");
    healthStatus.textContent = "API Online";
  } catch (_err) {
    healthStatus.textContent = "API Offline";
  }
}

async function loadHistory() {
  try {
    const risk = riskFilter.value;
    const query = risk === "ALL" ? "?limit=30" : `?limit=30&risk=${encodeURIComponent(risk)}`;
    const response = await fetch(`/api/history${query}`);
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || "Unable to load history");
    }
    renderHistory(Array.isArray(data.records) ? data.records : []);
  } catch (err) {
    showError(err.message || "Unable to load history");
  }
}

async function clearHistory() {
  clearError();
  if (!window.confirm("Clear all scan history? This cannot be undone.")) {
    return;
  }

  try {
    const response = await fetch("/api/history", { method: "DELETE" });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || "Unable to clear history");
    }
    await loadHistory();
  } catch (err) {
    showError(err.message || "Unable to clear history");
  }
}

analyzeForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearError();

  const url = urlInput.value.trim();
  if (!url) {
    showError("Please enter a URL.");
    return;
  }

  try {
    const response = await fetch("/api/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url })
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error || "Scan failed");
    }

    renderResult(data);
    await loadHistory();
  } catch (err) {
    showError(err.message || "Scan failed");
  }
});

refreshHistoryBtn.addEventListener("click", loadHistory);
clearHistoryBtn.addEventListener("click", clearHistory);
riskFilter.addEventListener("change", loadHistory);
themeToggle.addEventListener("click", () => {
  const next = currentTheme() === "dark" ? "light" : "dark";
  applyTheme(next);
  localStorage.setItem(THEME_KEY, next);
});

initializeTheme();
initializeNavBehavior();
initializeRevealAnimation();
checkHealth();
loadHistory();

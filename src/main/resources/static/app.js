const TOKEN_KEY = "ra_jwt";

function $(sel) {
    return document.querySelector(sel);
}

function authHeaders() {
    const token = localStorage.getItem(TOKEN_KEY);
    const headers = {"Accept": "application/json"};
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }
    return headers;
}

async function parseError(res) {
    try {
        const data = await res.json();
        if (data && data.message) {
            return data.message;
        }
    } catch (e) {
        /* ignore */
    }
    return res.statusText || "Request failed";
}

function showAuth() {
    $("#auth-section").classList.remove("hidden");
    $("#app-section").classList.add("hidden");
}

function showApp(email) {
    $("#auth-section").classList.add("hidden");
    $("#app-section").classList.remove("hidden");
    $("#user-email").textContent = email || "";
    loadHistory();
}

function setStatus(el, message, type) {
    el.textContent = message || "";
    el.classList.remove("error", "ok");
    if (type) {
        el.classList.add(type);
    }
}

document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        const name = tab.getAttribute("data-tab");
        $("#login-form").classList.toggle("hidden", name !== "login");
        $("#register-form").classList.toggle("hidden", name !== "register");
    });
});

$("#login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const payload = {
        email: form.email.value.trim(),
        password: form.password.value,
    };
    setStatus($("#auth-status"), "Signing in…", null);
    try {
        const res = await fetch("/api/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        const data = await res.json();
        localStorage.setItem(TOKEN_KEY, data.token);
        setStatus($("#auth-status"), "Welcome back.", "ok");
        showApp(data.email);
    } catch (err) {
        setStatus($("#auth-status"), err.message, "error");
    }
});

$("#register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const payload = {
        email: form.email.value.trim(),
        password: form.password.value,
    };
    setStatus($("#auth-status"), "Creating account…", null);
    try {
        const res = await fetch("/api/auth/register", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload),
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        const data = await res.json();
        localStorage.setItem(TOKEN_KEY, data.token);
        setStatus($("#auth-status"), "Account created.", "ok");
        showApp(data.email);
    } catch (err) {
        setStatus($("#auth-status"), err.message, "error");
    }
});

$("#logout-btn").addEventListener("click", () => {
    localStorage.removeItem(TOKEN_KEY);
    showAuth();
});

$("#upload-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const input = $("#file-input");
    if (!input.files.length) {
        setStatus($("#upload-status"), "Choose a file first.", "error");
        return;
    }
    const formData = new FormData();
    formData.append("file", input.files[0]);
    const btn = $("#analyze-btn");
    btn.disabled = true;
    setStatus($("#upload-status"), "Analyzing… this may take a minute.", null);
    try {
        const res = await fetch("/api/resumes/analyze", {
            method: "POST",
            headers: authHeaders(),
            body: formData,
        });
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        const data = await res.json();
        renderResults(data);
        setStatus($("#upload-status"), "Analysis complete.", "ok");
        loadHistory();
    } catch (err) {
        setStatus($("#upload-status"), err.message, "error");
    } finally {
        btn.disabled = false;
    }
});

function renderResults(data) {
    $("#results-empty").classList.add("hidden");
    $("#results-content").classList.remove("hidden");
    const ats = data.atsScore != null ? String(data.atsScore) : "—";
    $("#ats-score").textContent = ats;
    $("#summary").textContent = data.summary || "";

    const skills = $("#skills-list");
    skills.innerHTML = "";
    (data.skills || []).forEach((s) => {
        const li = document.createElement("li");
        li.textContent = s;
        skills.appendChild(li);
    });

    const impr = $("#improvements-list");
    impr.innerHTML = "";
    (data.improvements || []).forEach((item) => {
        const li = document.createElement("li");
        const strong = document.createElement("strong");
        strong.textContent = item.area || "Suggestion";
        const span = document.createElement("span");
        span.textContent = item.suggestion || "";
        li.appendChild(strong);
        li.appendChild(span);
        impr.appendChild(li);
    });
}

$("#refresh-history").addEventListener("click", loadHistory);

async function loadHistory() {
    const tbody = $("#history-table tbody");
    try {
        const res = await fetch("/api/resumes/history?limit=20", {headers: authHeaders()});
        if (!res.ok) {
            throw new Error(await parseError(res));
        }
        const items = await res.json();
        tbody.innerHTML = "";
        if (!items.length) {
            $("#history-empty").classList.remove("hidden");
            $("#history-table").classList.add("hidden");
            return;
        }
        $("#history-empty").classList.add("hidden");
        $("#history-table").classList.remove("hidden");
        items.forEach((row) => {
            const tr = document.createElement("tr");
            const when = new Date(row.createdAt).toLocaleString();
            tr.innerHTML = `<td>${when}</td><td>${escapeHtml(row.originalFilename)}</td><td>${row.atsScore ?? "—"}</td>`;
            tbody.appendChild(tr);
        });
    } catch (e) {
        /* silent on history failure */
    }
}

function escapeHtml(str) {
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

(function init() {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
        try {
            const payload = JSON.parse(atob(token.split(".")[1]));
            const email = payload.sub;
            showApp(email);
        } catch (e) {
            localStorage.removeItem(TOKEN_KEY);
            showAuth();
        }
    } else {
        showAuth();
    }
})();

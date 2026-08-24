/*
 * Smart Resume Screener - dashboard.
 * Plain ES modules-free JavaScript so the page is served straight from Spring Boot with no build step.
 */
(function () {
    "use strict";

    const API = "/api/v1";

    const state = {
        jobs: [],
        resumes: [],
        selectedJobId: null
    };

    const el = {
        engineDot: document.getElementById("engine-dot"),
        engineText: document.getElementById("engine-text"),
        jobForm: document.getElementById("job-form"),
        jobSubmit: document.getElementById("job-submit"),
        jobList: document.getElementById("job-list"),
        dropzone: document.getElementById("dropzone"),
        fileInput: document.getElementById("file-input"),
        resumeList: document.getElementById("resume-list"),
        screenBtn: document.getElementById("screen-btn"),
        minScore: document.getElementById("min-score"),
        topN: document.getElementById("top-n"),
        refresh: document.getElementById("refresh"),
        results: document.getElementById("results"),
        toast: document.getElementById("toast")
    };

    // ---------------------------------------------------------------- helpers

    function toast(message, isError) {
        el.toast.textContent = message;
        el.toast.classList.toggle("error", Boolean(isError));
        el.toast.classList.add("show");
        window.clearTimeout(toast.timer);
        toast.timer = window.setTimeout(function () {
            el.toast.classList.remove("show");
        }, 4200);
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return "";
        }
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    /**
     * Turns an RFC 7807 problem response into something a human can act on. Field-level validation
     * errors are named explicitly, because "one or more fields are invalid" helps nobody.
     */
    function describeError(payload, status) {
        if (payload && payload.fieldErrors) {
            const fields = Object.keys(payload.fieldErrors);
            if (fields.length > 0) {
                return fields
                    .map(function (field) { return field + ": " + payload.fieldErrors[field]; })
                    .join(" | ");
            }
        }
        if (payload && (payload.detail || payload.message)) {
            return payload.detail || payload.message;
        }
        return "Request failed with status " + status;
    }

    async function request(path, options) {
        const response = await fetch(API + path, options);
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        const payload = text ? JSON.parse(text) : null;
        if (!response.ok) {
            throw new Error(describeError(payload, response.status));
        }
        return payload;
    }

    function chips(values, className) {
        if (!values || values.length === 0) {
            return "";
        }
        return values
            .map(function (value) {
                return '<span class="chip ' + className + '">' + escapeHtml(value) + "</span>";
            })
            .join("");
    }

    // ---------------------------------------------------------------- status

    async function loadStatus() {
        try {
            const status = await request("/system/status");
            const usingModel = status.llmConfigured;
            el.engineDot.classList.toggle("offline", !usingModel);
            el.engineText.textContent = usingModel
                ? "LLM scoring: " + status.model
                : "Rule-based scoring (set GEMINI_API_KEY for LLM scoring)";
        } catch (error) {
            el.engineDot.classList.add("offline");
            el.engineText.textContent = "API unreachable";
        }
    }

    // ---------------------------------------------------------------- jobs

    async function loadJobs() {
        state.jobs = (await request("/jobs")) || [];
        renderJobs();
    }

    function renderJobs() {
        if (state.jobs.length === 0) {
            el.jobList.innerHTML = '<p class="empty">No job descriptions yet.</p>';
            updateScreenButton();
            return;
        }
        el.jobList.innerHTML = state.jobs
            .map(function (job) {
                const selected = job.id === state.selectedJobId ? " selected" : "";
                const meta = [job.company, job.location].filter(Boolean).join(" - ");
                const years = job.minYearsExperience ? job.minYearsExperience + "+ yrs" : "";
                return (
                    '<div class="list-item selectable' + selected + '" data-job="' + job.id + '">' +
                    "<div>" +
                    "<h4>" + escapeHtml(job.title) + "</h4>" +
                    "<p>" + escapeHtml([meta, years].filter(Boolean).join(" - ") || "No company details") + "</p>" +
                    '<div class="chips">' + chips((job.requiredSkills || []).slice(0, 8), "") + "</div>" +
                    "</div>" +
                    '<button class="icon-btn" data-delete-job="' + job.id + '" title="Delete">&times;</button>' +
                    "</div>"
                );
            })
            .join("");
        updateScreenButton();
    }

    el.jobList.addEventListener("click", async function (event) {
        const deleteId = event.target.getAttribute("data-delete-job");
        if (deleteId) {
            event.stopPropagation();
            try {
                await request("/jobs/" + deleteId, { method: "DELETE" });
                if (state.selectedJobId === deleteId) {
                    state.selectedJobId = null;
                    el.results.innerHTML = '<p class="empty">Results will appear here.</p>';
                }
                await loadJobs();
                toast("Job description deleted.");
            } catch (error) {
                toast(error.message, true);
            }
            return;
        }
        const item = event.target.closest("[data-job]");
        if (item) {
            state.selectedJobId = item.getAttribute("data-job");
            renderJobs();
        }
    });

    el.jobForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        const skills = document.getElementById("job-skills").value
            .split(",")
            .map(function (skill) { return skill.trim(); })
            .filter(Boolean);
        const minYears = document.getElementById("job-min-years").value;

        const body = {
            title: document.getElementById("job-title").value.trim(),
            company: document.getElementById("job-company").value.trim() || null,
            location: document.getElementById("job-location").value.trim() || null,
            minYearsExperience: minYears ? Number(minYears) : null,
            description: document.getElementById("job-description").value.trim(),
            requiredSkills: skills
        };

        el.jobSubmit.disabled = true;
        el.jobSubmit.textContent = "Saving...";
        try {
            const created = await request("/jobs", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body)
            });
            state.selectedJobId = created.id;
            el.jobForm.reset();
            await loadJobs();
            toast('Saved "' + created.title + '" with ' + (created.requiredSkills || []).length + " required skills.");
        } catch (error) {
            toast(error.message, true);
        } finally {
            el.jobSubmit.disabled = false;
            el.jobSubmit.textContent = "Save job description";
        }
    });

    // ---------------------------------------------------------------- resumes

    async function loadResumes() {
        state.resumes = (await request("/resumes")) || [];
        renderResumes();
    }

    function renderResumes() {
        if (state.resumes.length === 0) {
            el.resumeList.innerHTML = '<p class="empty">No resumes uploaded yet.</p>';
            updateScreenButton();
            return;
        }
        el.resumeList.innerHTML = state.resumes
            .map(function (resume) {
                const facts = [
                    resume.yearsOfExperience ? resume.yearsOfExperience + " yrs" : null,
                    resume.highestEducation,
                    resume.email
                ].filter(Boolean).join(" - ");
                return (
                    '<div class="list-item">' +
                    "<div>" +
                    "<h4>" + escapeHtml(resume.candidateName || resume.fileName) + "</h4>" +
                    "<p>" + escapeHtml(facts || resume.fileName) + "</p>" +
                    '<div class="chips">' + chips((resume.skills || []).slice(0, 10), "") + "</div>" +
                    "</div>" +
                    '<button class="icon-btn" data-delete-resume="' + resume.id + '" title="Delete">&times;</button>' +
                    "</div>"
                );
            })
            .join("");
        updateScreenButton();
    }

    el.resumeList.addEventListener("click", async function (event) {
        const deleteId = event.target.getAttribute("data-delete-resume");
        if (!deleteId) {
            return;
        }
        try {
            await request("/resumes/" + deleteId, { method: "DELETE" });
            await loadResumes();
            toast("Resume deleted.");
        } catch (error) {
            toast(error.message, true);
        }
    });

    el.dropzone.addEventListener("click", function () {
        el.fileInput.click();
    });

    el.dropzone.addEventListener("dragover", function (event) {
        event.preventDefault();
        el.dropzone.classList.add("dragover");
    });

    el.dropzone.addEventListener("dragleave", function () {
        el.dropzone.classList.remove("dragover");
    });

    el.dropzone.addEventListener("drop", function (event) {
        event.preventDefault();
        el.dropzone.classList.remove("dragover");
        uploadFiles(event.dataTransfer.files);
    });

    el.fileInput.addEventListener("change", function () {
        uploadFiles(el.fileInput.files);
        el.fileInput.value = "";
    });

    async function uploadFiles(fileList) {
        if (!fileList || fileList.length === 0) {
            return;
        }
        const form = new FormData();
        for (let i = 0; i < fileList.length; i++) {
            form.append("files", fileList[i]);
        }

        el.dropzone.textContent = "Parsing " + fileList.length + " file(s)...";
        try {
            const result = await request("/resumes", { method: "POST", body: form });
            await loadResumes();
            const rejected = result.rejected || [];
            if (rejected.length > 0) {
                toast(result.parsed + " parsed, " + rejected.length + " rejected: " + rejected[0].reason, true);
            } else {
                toast(result.parsed + " resume(s) parsed.");
            }
        } catch (error) {
            toast(error.message, true);
        } finally {
            el.dropzone.innerHTML =
                "<strong>Drop resumes here or click to browse</strong>" +
                "Skills, experience and education are extracted automatically";
        }
    }

    // ---------------------------------------------------------------- screening

    function updateScreenButton() {
        el.screenBtn.disabled = !state.selectedJobId || state.resumes.length === 0;
    }

    el.screenBtn.addEventListener("click", async function () {
        if (!state.selectedJobId) {
            toast("Select a job description first.", true);
            return;
        }
        const body = {
            jobDescriptionId: state.selectedJobId,
            minScore: el.minScore.value ? Number(el.minScore.value) : null,
            topN: el.topN.value ? Number(el.topN.value) : null,
            refresh: el.refresh.checked
        };

        el.screenBtn.disabled = true;
        el.screenBtn.textContent = "Screening...";
        el.results.innerHTML = '<p class="empty">Scoring candidates, this can take a few seconds per resume...</p>';
        try {
            const response = await request("/screenings", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body)
            });
            renderResults(response);
        } catch (error) {
            el.results.innerHTML = '<p class="empty">Screening failed.</p>';
            toast(error.message, true);
        } finally {
            el.screenBtn.textContent = "Screen candidates";
            updateScreenButton();
        }
    });

    function renderResults(response) {
        const results = response.results || [];
        if (results.length === 0) {
            el.results.innerHTML = '<p class="empty">No candidates matched the filters.</p>';
            return;
        }

        const header =
            '<p class="hint">' + response.candidatesEvaluated + " evaluated, " +
            response.candidatesReturned + " shown, " + response.shortlistedCount +
            " recommended for shortlist - engine: " + escapeHtml(response.engine) + "</p>";

        el.results.innerHTML = header + results.map(renderCandidate).join("");
    }

    function renderCandidate(candidate) {
        const percent = Math.round(candidate.matchScore * 10);
        const badgeClass = String(candidate.recommendation || "").toLowerCase();
        const label = String(candidate.recommendation || "").replace(/_/g, " ").toLowerCase();
        const years = candidate.yearsOfExperience ? candidate.yearsOfExperience + " yrs" : "experience unknown";
        const source = candidate.analysisSource === "LLM" ? "LLM" : "rule engine";

        return (
            '<article class="result' + (candidate.rank === 1 ? " top" : "") + '">' +
            '<div class="score-ring" style="--pct:' + percent + '"><span>' + candidate.matchScore.toFixed(1) + "</span></div>" +
            "<div>" +
            '<div class="result-head">' +
            "<div>" +
            "<h3>#" + candidate.rank + " " + escapeHtml(candidate.candidateName) + "</h3>" +
            '<div class="meta">' + escapeHtml([candidate.email, years, candidate.fileName].filter(Boolean).join(" - ")) + "</div>" +
            "</div>" +
            '<span class="badge ' + badgeClass + '">' + escapeHtml(label) + "</span>" +
            "</div>" +
            '<div class="subscores">' +
            "<span>Skills <b>" + candidate.skillScore.toFixed(1) + "</b></span>" +
            "<span>Experience <b>" + candidate.experienceScore.toFixed(1) + "</b></span>" +
            "<span>Education <b>" + candidate.educationScore.toFixed(1) + "</b></span>" +
            "<span>Scored by <b>" + source + "</b></span>" +
            "</div>" +
            '<div class="chips">' +
            chips(candidate.matchedSkills, "match") +
            chips(candidate.missingSkills, "missing") +
            "</div>" +
            '<p class="justification">' + escapeHtml(candidate.justification) + "</p>" +
            "</div>" +
            "</article>"
        );
    }

    // ---------------------------------------------------------------- boot

    (async function init() {
        await loadStatus();
        try {
            await Promise.all([loadJobs(), loadResumes()]);
        } catch (error) {
            toast(error.message, true);
        }
    })();
})();

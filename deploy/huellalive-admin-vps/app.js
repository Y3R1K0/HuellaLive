const DEFAULT_API = "https://api.huellalive.lat";

const state = {
  apiBase: localStorage.getItem("hl_admin_api") || DEFAULT_API,
  token: localStorage.getItem("hl_admin_token") || "",
  user: JSON.parse(localStorage.getItem("hl_admin_user") || "null"),
  tab: "overview",
  sheltersById: new Map(),
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const titles = {
  overview: "Resumen",
  shelters: "Albergues pendientes",
  cities: "Ciudades predeterminadas",
  species: "Especies predeterminadas",
  speciesRequests: "Solicitudes de especie",
  reports: "Reportes",
};

function init() {
  $("#apiBaseInput").value = state.apiBase;
  $("#loginForm").addEventListener("submit", login);
  $("#logoutBtn").addEventListener("click", logout);
  $("#refreshBtn").addEventListener("click", () => loadTab(state.tab));
  $$(".nav button").forEach((button) => {
    button.addEventListener("click", () => setTab(button.dataset.tab));
  });

  if (state.token && state.user?.role === "ADMIN") showDashboard();
  else showLogin();
}

async function login(event) {
  event.preventDefault();
  $("#loginMessage").textContent = "";
  state.apiBase = cleanApiBase($("#apiBaseInput").value);
  localStorage.setItem("hl_admin_api", state.apiBase);

  try {
    const response = await fetch(`${state.apiBase}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: $("#emailInput").value,
        password: $("#passwordInput").value,
      }),
    });
    const data = await parseResponse(response);
    if (data.user?.role !== "ADMIN") throw new Error("Esta cuenta no tiene permisos de administrador.");
    state.token = data.accessToken;
    state.user = data.user;
    localStorage.setItem("hl_admin_token", state.token);
    localStorage.setItem("hl_admin_user", JSON.stringify(state.user));
    showDashboard();
  } catch (error) {
    $("#loginMessage").textContent = error.message;
  }
}

function logout() {
  state.token = "";
  state.user = null;
  localStorage.removeItem("hl_admin_token");
  localStorage.removeItem("hl_admin_user");
  showLogin();
}

function showLogin() {
  $("#loginView").classList.remove("hidden");
  $("#dashboardView").classList.add("hidden");
}

function showDashboard() {
  $("#loginView").classList.add("hidden");
  $("#dashboardView").classList.remove("hidden");
  setTab(state.tab);
}

function setTab(tab) {
  state.tab = tab;
  $("#pageTitle").textContent = titles[tab];
  $$(".nav button").forEach((button) => button.classList.toggle("active", button.dataset.tab === tab));
  $$(".tab-page").forEach((page) => page.classList.toggle("hidden", page.id !== tab));
  loadTab(tab);
}

async function loadTab(tab) {
  const loaders = {
    overview: loadOverview,
    shelters: loadShelters,
    cities: loadCities,
    species: loadSpecies,
    speciesRequests: loadSpeciesRequests,
    reports: loadReports,
  };
  await loaders[tab]();
}

async function api(path, options = {}) {
  const response = await fetch(`${state.apiBase}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${state.token}`,
      ...(options.headers || {}),
    },
  });
  return parseResponse(response);
}

async function parseResponse(response) {
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || data?.error || `Error ${response.status}`);
  return data;
}

function cleanApiBase(value) {
  return (value || DEFAULT_API).trim().replace(/\/+$/, "");
}

function toast(message) {
  const el = $("#toast");
  el.textContent = message;
  el.classList.remove("hidden");
  setTimeout(() => el.classList.add("hidden"), 2800);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function money(value) {
  return `S/ ${Number(value || 0).toFixed(2)}`;
}

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("es-PE", { dateStyle: "medium" }).format(new Date(value));
}

async function withAction(action, successMessage) {
  try {
    await action();
    toast(successMessage);
    await loadTab(state.tab);
  } catch (error) {
    toast(error.message);
  }
}

async function loadOverview() {
  const root = $("#overview");
  root.innerHTML = `<div class="table-card">Cargando metricas...</div>`;
  try {
    const stats = await api("/admin/stats");
    root.innerHTML = `
      <div class="grid stats-grid">
        ${stat("Usuarios", stats.users)}
        ${stat("Albergues pendientes", stats.sheltersPending)}
        ${stat("Albergues aprobados", stats.sheltersApproved)}
        ${stat("Animales", stats.animals)}
        ${stat("Adopciones", stats.adoptions)}
        ${stat("Donaciones completadas", money(stats.completedDonationsTotal))}
      </div>
    `;
  } catch (error) {
    root.innerHTML = errorBox(error.message);
  }
}

function stat(label, value) {
  return `<article class="stat-card"><span>${label}</span><strong>${value}</strong></article>`;
}

async function loadShelters() {
  const root = $("#shelters");
  root.innerHTML = `<div class="table-card">Cargando albergues...</div>`;
  try {
    const [allShelters, pendingShelters] = await Promise.all([
      api("/admin/shelters"),
      api("/admin/shelters/pending"),
    ]);
    state.sheltersById = new Map(allShelters.map((shelter) => [shelter.id, shelter]));
    root.innerHTML = `
      <div class="grid stats-grid">
        ${stat("Registrados", allShelters.length)}
        ${stat("Pendientes", pendingShelters.length)}
        ${stat("Aprobados", allShelters.filter((item) => item.status === "APPROVED").length)}
        ${stat("Rechazados", allShelters.filter((item) => item.status === "REJECTED").length)}
      </div>
      <div class="table-card" style="margin-top:14px">
        <h3>Albergues existentes</h3>
        ${allShelters.length ? shelterTable(allShelters) : '<p class="muted">No hay albergues registrados.</p>'}
      </div>
      <div class="table-card" style="margin-top:14px">
        <h3>Pendientes de aprobacion</h3>
        ${pendingShelters.length ? `<div class="cards">${pendingShelters.map(shelterCard).join("")}</div>` : '<p class="muted">No hay albergues pendientes.</p>'}
      </div>
    `;
    root.querySelectorAll("[data-approve]").forEach((button) => {
      button.addEventListener("click", () => withAction(
        () => api(`/admin/shelters/${button.dataset.approve}/approve`, { method: "PATCH" }),
        "Albergue aprobado"
      ));
    });
    root.querySelectorAll("[data-reject]").forEach((button) => {
      button.addEventListener("click", () => withAction(
        () => api(`/admin/shelters/${button.dataset.reject}/reject`, { method: "PATCH" }),
        "Albergue rechazado"
      ));
    });
    root.querySelectorAll("[data-edit-shelter]").forEach((button) => {
      button.addEventListener("click", () => editShelter(button.dataset.editShelter));
    });
    root.querySelectorAll("[data-status-shelter]").forEach((select) => {
      select.addEventListener("change", () => withAction(
        () => api(`/admin/shelters/${select.dataset.statusShelter}`, {
          method: "PATCH",
          body: JSON.stringify({ status: select.value }),
        }),
        "Estado actualizado"
      ));
    });
    root.querySelectorAll("[data-toggle-shelter]").forEach((button) => {
      button.addEventListener("click", () => withAction(
        () => api(`/admin/shelters/${button.dataset.toggleShelter}`, {
          method: "PATCH",
          body: JSON.stringify({ isActive: button.dataset.active !== "true" }),
        }),
        "Acceso actualizado"
      ));
    });
    root.querySelectorAll("[data-delete-shelter]").forEach((button) => {
      button.addEventListener("click", () => {
        if (!confirm("Esto desactivara la cuenta del albergue y lo marcara como rechazado. Continuar?")) return;
        withAction(
          () => api(`/admin/shelters/${button.dataset.deleteShelter}`, { method: "DELETE" }),
          "Albergue desactivado"
        );
      });
    });
  } catch (error) {
    root.innerHTML = errorBox(error.message);
  }
}

function editShelter(id) {
  const shelter = state.sheltersById.get(id);
  if (!shelter) return;
  const name = prompt("Nombre del albergue", shelter.user?.name || "");
  if (name === null) return;
  const email = prompt("Correo", shelter.user?.email || "");
  if (email === null) return;
  const phone = prompt("Telefono", shelter.phone || "");
  if (phone === null) return;
  const location = prompt("Direccion/ciudad", shelter.location || "");
  if (location === null) return;
  const description = prompt("Descripcion", shelter.description || "");
  if (description === null) return;
  const avatarUrl = prompt("URL foto de perfil", shelter.user?.avatarUrl || "");
  if (avatarUrl === null) return;
  const coverUrl = prompt("URL portada", shelter.coverUrl || "");
  if (coverUrl === null) return;
  const latitude = prompt("Latitud", shelter.latitude ?? "");
  if (latitude === null) return;
  const longitude = prompt("Longitud", shelter.longitude ?? "");
  if (longitude === null) return;

  return withAction(
    () => api(`/admin/shelters/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        name,
        email,
        phone,
        location,
        description,
        avatarUrl,
        coverUrl,
        latitude,
        longitude,
      }),
    }),
    "Albergue actualizado"
  );
}

function shelterTable(shelters) {
  return `
    <table>
      <thead>
        <tr>
          <th>Albergue</th>
          <th>Correo</th>
          <th>Estado</th>
          <th>Ciudad/direccion</th>
          <th>Animales</th>
          <th>Creado</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        ${shelters.map((shelter) => `
          <tr>
            <td>
              <div class="item-head">
                <img class="avatar" src="${escapeHtml(shelter.user?.avatarUrl || "")}" onerror="this.style.display='none'" />
                <div>
                  <strong>${escapeHtml(shelter.user?.name)}</strong>
                  <p class="item-sub">${escapeHtml(shelter.phone || "Sin telefono")}</p>
                </div>
              </div>
            </td>
            <td>${escapeHtml(shelter.user?.email)}</td>
            <td>
              <select data-status-shelter="${shelter.id}">
                ${["PENDING", "APPROVED", "REJECTED"].map((status) => `
                  <option value="${status}" ${shelter.status === status ? "selected" : ""}>${status}</option>
                `).join("")}
              </select>
              <p class="item-sub">${shelter.user?.isActive === false ? "Cuenta inactiva" : "Cuenta activa"}</p>
            </td>
            <td>${escapeHtml(shelter.location || "Sin ubicacion")}</td>
            <td>${escapeHtml(shelter._count?.animals ?? 0)}</td>
            <td>${formatDate(shelter.createdAt)}</td>
            <td>
              <div class="actions">
                <button class="secondary-btn" data-edit-shelter="${shelter.id}">Editar</button>
                <button class="secondary-btn" data-toggle-shelter="${shelter.id}" data-active="${shelter.user?.isActive !== false}">
                  ${shelter.user?.isActive === false ? "Activar" : "Desactivar"}
                </button>
                <button class="danger-btn" data-delete-shelter="${shelter.id}">Eliminar</button>
              </div>
            </td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function shelterCard(shelter) {
  return `
    <article class="item-card">
      <div class="item-head">
        <img class="avatar" src="${escapeHtml(shelter.user?.avatarUrl || "")}" onerror="this.style.display='none'" />
        <div>
          <h3 class="item-title">${escapeHtml(shelter.user?.name)}</h3>
          <p class="item-sub">${escapeHtml(shelter.user?.email)}</p>
        </div>
      </div>
      <p class="muted">${escapeHtml(shelter.description || "Sin descripcion")}</p>
      <p><span class="pill">${escapeHtml(shelter.location || "Sin ubicacion")}</span></p>
      <div class="actions">
        <button class="success-btn" data-approve="${shelter.id}">Aprobar</button>
        <button class="danger-btn" data-reject="${shelter.id}">Rechazar</button>
      </div>
    </article>
  `;
}

async function loadCities() {
  await loadCatalog({
    root: $("#cities"),
    endpoint: "/admin/search-cities",
    title: "Nueva ciudad",
    hasRegion: true,
    toolbarClass: "",
  });
}

async function loadSpecies() {
  await loadCatalog({
    root: $("#species"),
    endpoint: "/admin/search-species",
    title: "Nueva especie",
    hasRegion: false,
    toolbarClass: "species",
  });
}

async function loadCatalog(config) {
  config.root.innerHTML = `<div class="table-card">Cargando...</div>`;
  try {
    const rows = await api(config.endpoint);
    config.root.innerHTML = `
      <form class="form-card toolbar ${config.toolbarClass}" data-create-catalog>
        <input name="name" placeholder="${config.title}" required />
        ${config.hasRegion ? '<input name="region" placeholder="Region" />' : ""}
        <input name="sortOrder" type="number" placeholder="Orden" value="0" />
        <select name="isActive">
          <option value="true">Activo</option>
          <option value="false">Inactivo</option>
        </select>
        <button class="primary-btn" type="submit">Crear</button>
      </form>
      <div class="table-card">
        <table>
          <thead>
            <tr>
              <th>Nombre</th>
              ${config.hasRegion ? "<th>Region</th>" : ""}
              <th>Orden</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>${rows.map((row) => catalogRow(row, config.hasRegion)).join("")}</tbody>
        </table>
      </div>
    `;
    config.root.querySelector("[data-create-catalog]").addEventListener("submit", (event) => {
      event.preventDefault();
      const form = new FormData(event.currentTarget);
      const body = {
        name: form.get("name"),
        sortOrder: Number(form.get("sortOrder") || 0),
        isActive: form.get("isActive") === "true",
      };
      if (config.hasRegion) body.region = form.get("region");
      withAction(() => api(config.endpoint, { method: "POST", body: JSON.stringify(body) }), "Elemento creado");
    });
    config.root.querySelectorAll("[data-toggle]").forEach((button) => {
      button.addEventListener("click", () => withAction(
        () => api(`${config.endpoint}/${button.dataset.toggle}`, {
          method: "PATCH",
          body: JSON.stringify({ isActive: button.dataset.active !== "true" }),
        }),
        "Estado actualizado"
      ));
    });
    config.root.querySelectorAll("[data-delete]").forEach((button) => {
      button.addEventListener("click", () => {
        if (!confirm("Seguro que quieres eliminar este elemento?")) return;
        withAction(() => api(`${config.endpoint}/${button.dataset.delete}`, { method: "DELETE" }), "Elemento eliminado");
      });
    });
  } catch (error) {
    config.root.innerHTML = errorBox(error.message);
  }
}

function catalogRow(row, hasRegion) {
  return `
    <tr>
      <td>${escapeHtml(row.name)}</td>
      ${hasRegion ? `<td>${escapeHtml(row.region || "")}</td>` : ""}
      <td>${escapeHtml(row.sortOrder)}</td>
      <td><span class="pill">${row.isActive ? "Activo" : "Inactivo"}</span></td>
      <td>
        <div class="actions">
          <button class="secondary-btn" data-toggle="${row.id}" data-active="${row.isActive}">
            ${row.isActive ? "Desactivar" : "Activar"}
          </button>
          <button class="danger-btn" data-delete="${row.id}">Eliminar</button>
        </div>
      </td>
    </tr>
  `;
}

async function loadSpeciesRequests() {
  const root = $("#speciesRequests");
  root.innerHTML = `<div class="table-card">Cargando solicitudes...</div>`;
  try {
    const requests = await api("/admin/species-requests");
    if (!requests.length) {
      root.innerHTML = empty("No hay solicitudes de especie pendientes.");
      return;
    }
    root.innerHTML = `
      <div class="cards">
        ${requests.map((request) => `
          <article class="item-card">
            <p class="eyebrow">Nueva especie</p>
            <h3 class="item-title">${escapeHtml(request.requestedName)}</h3>
            <p class="item-sub">Animal: ${escapeHtml(request.animal?.name || "Sin animal")}</p>
            <p class="item-sub">Albergue: ${escapeHtml(request.shelter?.user?.name || "Sin albergue")}</p>
            <div class="actions">
              <button class="success-btn" data-approve-species="${request.id}">Aprobar</button>
              <button class="danger-btn" data-reject-species="${request.id}">Rechazar</button>
            </div>
          </article>
        `).join("")}
      </div>
    `;
    root.querySelectorAll("[data-approve-species]").forEach((button) => {
      button.addEventListener("click", () => withAction(
        () => api(`/admin/species-requests/${button.dataset.approveSpecies}/approve`, { method: "PATCH" }),
        "Solicitud aprobada"
      ));
    });
    root.querySelectorAll("[data-reject-species]").forEach((button) => {
      button.addEventListener("click", () => {
        const reason = prompt("Motivo del rechazo") || "";
        withAction(
          () => api(`/admin/species-requests/${button.dataset.rejectSpecies}/reject`, {
            method: "PATCH",
            body: JSON.stringify({ reason }),
          }),
          "Solicitud rechazada"
        );
      });
    });
  } catch (error) {
    root.innerHTML = errorBox(error.message);
  }
}

async function loadReports() {
  const root = $("#reports");
  root.innerHTML = `<div class="table-card">Cargando reportes...</div>`;
  try {
    const reports = await api("/admin/reports");
    if (!reports.length) {
      root.innerHTML = empty("No hay reportes.");
      return;
    }
    root.innerHTML = `
      <div class="table-card">
        <table>
          <thead>
            <tr>
              <th>Estado</th>
              <th>Reporta</th>
              <th>Motivo</th>
              <th>Animal</th>
              <th>Accion</th>
            </tr>
          </thead>
          <tbody>${reports.map(reportRow).join("")}</tbody>
        </table>
      </div>
    `;
    root.querySelectorAll("[data-report-status]").forEach((select) => {
      select.addEventListener("change", () => withAction(
        () => api(`/admin/reports/${select.dataset.reportStatus}`, {
          method: "PATCH",
          body: JSON.stringify({ status: select.value }),
        }),
        "Reporte actualizado"
      ));
    });
  } catch (error) {
    root.innerHTML = errorBox(error.message);
  }
}

function reportRow(report) {
  const options = ["PENDING", "REVIEWED", "DISMISSED", "ACTION_TAKEN"]
    .map((status) => `<option value="${status}" ${report.status === status ? "selected" : ""}>${status}</option>`)
    .join("");
  return `
    <tr>
      <td><span class="pill">${escapeHtml(report.status)}</span></td>
      <td>${escapeHtml(report.reporterName)}<br /><span class="item-sub">${escapeHtml(report.reporterEmail)}</span></td>
      <td>${escapeHtml(report.reason || report.type || "Sin motivo")}</td>
      <td>${escapeHtml(report.animalName || "Sin animal")}</td>
      <td><select data-report-status="${report.id}">${options}</select></td>
    </tr>
  `;
}

function empty(message) {
  return `<div class="table-card"><p class="muted">${message}</p></div>`;
}

function errorBox(message) {
  return `<div class="table-card"><p class="message">${escapeHtml(message)}</p></div>`;
}

init();

requireAuth('STUDENT');
document.getElementById('userEmail').textContent = getEmail();

setupTabs(tab => {
  if (tab === 'academics') loadRecords();
  if (tab === 'resume') { loadSkills(); loadResume(); }
  if (tab === 'documents') loadDocs();
  if (tab === 'browse') loadBrowse();
  if (tab === 'applications') loadApplications();
  if (tab === 'offers') loadOffers();
  if (tab === 'notifications') loadNotifications();
});

// ---------------- profile ----------------
async function loadProfile() {
  try {
    const s = await api('/students/me');
    document.getElementById('p-name').value = s.name || '';
    document.getElementById('p-department').value = s.department || '';
    document.getElementById('p-semester').value = s.semester ?? '';
    document.getElementById('p-cgpa').value = s.cgpa ?? '';
    document.getElementById('p-backlogs').value = s.backlogs ?? '';
    document.getElementById('p-phone').value = s.phone || '';
    document.getElementById('p-preferences').value = s.internshipPreferences || '';
    document.getElementById('p-certs').value = s.certifications || '';
    document.getElementById('p-projects').value = s.projects || '';
    document.getElementById('verifiedBadge').innerHTML = s.verified
      ? '<span class="badge badge-eligible">VERIFIED BY FACULTY</span>'
      : '<span class="badge bg-warning text-dark">NOT YET VERIFIED</span>';
  } catch (err) { showAlert('alert', err.message); }
}

document.getElementById('profileForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/students/me', 'PUT', {
      name: document.getElementById('p-name').value,
      department: document.getElementById('p-department').value,
      semester: num('p-semester'),
      cgpa: num('p-cgpa'),
      backlogs: num('p-backlogs'),
      phone: document.getElementById('p-phone').value,
      internshipPreferences: document.getElementById('p-preferences').value,
      certifications: document.getElementById('p-certs').value,
      projects: document.getElementById('p-projects').value
    });
    showAlert('alert', 'Profile saved. Faculty will need to re-verify any changed academic data.', 'success');
    loadProfile();
  } catch (err) { showAlert('alert', err.message); }
});

// ---------------- marksheet records ----------------
async function loadRecords() {
  const rows = await api('/students/me/academic-records');
  document.getElementById('recordsBody').innerHTML = rows.length === 0
    ? '<tr><td colspan="5" class="text-muted">No records yet.</td></tr>'
    : rows.map(r => `<tr><td>${esc(r.semester)}</td><td>${esc(r.subject)}</td><td>${esc(r.marks ?? '—')}</td><td>${esc(r.sgpa ?? '—')}</td>
        <td><button class="btn btn-sm btn-outline-danger" onclick="deleteRecord(${r.id})">Delete</button></td></tr>`).join('');
}

document.getElementById('recordForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/students/me/academic-records', 'POST', {
      semester: num('r-sem'), subject: document.getElementById('r-subject').value,
      marks: num('r-marks'), sgpa: num('r-sgpa')
    });
    e.target.reset();
    loadRecords(); loadProfile();
  } catch (err) { showAlert('alert', err.message); }
});

async function deleteRecord(id) {
  try { await api('/students/me/academic-records/' + id, 'DELETE'); loadRecords(); loadProfile(); }
  catch (err) { showAlert('alert', err.message); }
}

async function recalcCgpa() {
  try {
    const s = await api('/students/me/academic-records/recalculate-cgpa', 'POST');
    showAlert('alert', 'CGPA updated to ' + s.cgpa + '. Faculty must re-verify.', 'success');
    loadProfile();
  } catch (err) { showAlert('alert', err.message); }
}

// ---------------- skills & resume ----------------
async function loadSkills() {
  const skills = await api('/students/me/skills');
  document.getElementById('skillsInput').value = skills.map(s => s.skill.name).join(', ');
  document.getElementById('skillsList').innerHTML = skills.map(s =>
    `<span class="status-pill me-2 mb-2 d-inline-block">${esc(s.skill.name)}</span>`).join('');
}

document.getElementById('skillsForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const list = document.getElementById('skillsInput').value.split(',').map(s => s.trim()).filter(Boolean);
  try {
    await api('/students/me/skills', 'PUT', { skills: list });
    showAlert('alert', 'Skills updated.', 'success');
    loadSkills();
  } catch (err) { showAlert('alert', err.message); }
});

function renderResume(r) {
  document.getElementById('resumeResult').innerHTML = r
    ? `<div class="alert alert-success mb-0">Resume completeness score: <b>${esc(r.resumeScore)}</b>/100 &nbsp;·&nbsp;
         Extracted skills: <b>${esc(r.extractedSkills || '—')}</b>
         ${r.filePath ? ` &nbsp;·&nbsp; <a href="#" onclick="openFile('/students/me/resume/file');return false;">View uploaded file</a>` : ''}</div>`
    : '';
}
async function loadResume() { try { renderResume(await api('/students/me/resume')); } catch (e) { /* none yet */ } }

document.getElementById('resumeFileForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const fd = new FormData();
  fd.append('file', document.getElementById('resumeFile').files[0]);
  try { renderResume(await apiForm('/students/me/resume/file', fd)); showAlert('alert', 'Resume uploaded and analysed.', 'success'); }
  catch (err) { showAlert('alert', err.message); }
});

document.getElementById('resumeForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    renderResume(await api('/students/me/resume', 'POST', { rawText: document.getElementById('resumeText').value }));
    showAlert('alert', 'Resume text saved and analysed.', 'success');
  } catch (err) { showAlert('alert', err.message); }
});

// ---------------- documents ----------------
async function loadDocs() {
  const docs = await api('/students/me/documents');
  document.getElementById('docsBody').innerHTML = docs.length === 0
    ? '<tr><td colspan="3" class="text-muted">No documents uploaded.</td></tr>'
    : docs.map(d => `<tr><td>${esc(d.documentType)}</td><td>${pill(d.verificationStatus)}</td>
        <td><button class="btn btn-sm btn-outline-secondary" onclick="openFile('/students/me/documents/${d.id}/file')">View</button></td></tr>`).join('');
}

document.getElementById('docForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const fd = new FormData();
  fd.append('documentType', document.getElementById('d-type').value);
  fd.append('file', document.getElementById('d-file').files[0]);
  try { await apiForm('/students/me/documents', fd); e.target.reset(); showAlert('alert', 'Document uploaded.', 'success'); loadDocs(); }
  catch (err) { showAlert('alert', err.message); }
});

// ---------------- internships (ELIGIBLE / NOT ELIGIBLE) ----------------
async function loadBrowse() {
  const list = document.getElementById('browseList');
  list.innerHTML = '<p class="text-muted">Loading...</p>';
  try {
    const views = await api('/students/me/internships');
    if (views.length === 0) { list.innerHTML = '<p class="text-muted">No open internships right now.</p>'; return; }
    list.innerHTML = views.map(v => {
      const i = v.internship;
      const badge = v.eligible ? '<span class="badge badge-eligible">ELIGIBLE</span>' : '<span class="badge badge-ineligible">NOT ELIGIBLE</span>';
      const reasons = v.eligible ? '' : `<ul class="small text-danger mb-2">${v.reasons.map(r => '<li>' + esc(r) + '</li>').join('')}</ul>`;
      const c = i.eligibilityCriteria;
      const crit = c ? `Min CGPA ${esc(c.minCgpa)} · Max backlogs ${esc(c.maxBacklogs)}${c.eligibleBranches ? ' · ' + esc(c.eligibleBranches) : ''}${c.requiredSkills ? ' · Skills: ' + esc(c.requiredSkills) : ''}` : '';
      const action = v.applied ? '<button class="btn btn-sm btn-secondary" disabled>Applied</button>'
        : v.eligible ? `<button class="btn btn-sm btn-primary" onclick="applyTo(${i.id})">Apply</button>`
        : '<button class="btn btn-sm btn-secondary" disabled>Application disabled</button>';
      return `<div class="col-md-6"><div class="card card-portal p-3 h-100">
          <div class="d-flex justify-content-between"><h6 class="mb-1">${esc(i.role)}</h6>${badge}</div>
          <p class="small text-muted mb-1">${esc(i.company ? i.company.companyName : '')} · ${esc(i.location)} · ${esc(i.workMode)}</p>
          <p class="small mb-1">${esc(i.description)}</p>
          <p class="small mb-1"><b>Stipend:</b> ${esc(i.stipend ?? '—')} &nbsp; <b>Duration:</b> ${esc(i.duration ?? '—')} &nbsp; <b>Deadline:</b> ${esc(i.deadline ?? '—')}</p>
          <p class="small text-muted mb-2">${crit}</p>${reasons}${action}
        </div></div>`;
    }).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function applyTo(id) {
  try { await api('/students/me/applications/' + id, 'POST'); showAlert('alert', 'Application submitted.', 'success'); loadBrowse(); }
  catch (err) { showAlert('alert', err.message); }
}

// ---------------- applications ----------------
async function loadApplications() {
  const body = document.getElementById('applicationsBody');
  try {
    const apps = await api('/students/me/applications');
    body.innerHTML = apps.length === 0 ? '<tr><td colspan="6" class="text-muted">No applications yet.</td></tr>'
      : apps.map(a => `<tr>
          <td>${esc(a.internship.role)}</td>
          <td>${esc(a.internship.company ? a.internship.company.companyName : '')}</td>
          <td>${esc(fmtDate(a.applicationDate))}</td>
          <td>${a.resumeMatchScore != null ? esc(a.resumeMatchScore.toFixed(0)) + '%' : '—'}</td>
          <td>${pill(a.status)}</td>
          <td class="text-nowrap">
            <button class="btn btn-sm btn-outline-secondary" onclick="showInterviews(${a.id})">Interviews</button>
            ${['SELECTED', 'REJECTED', 'WITHDRAWN'].includes(a.status) ? '' : `<button class="btn btn-sm btn-outline-danger" onclick="withdraw(${a.id})">Withdraw</button>`}
          </td></tr>`).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function showInterviews(appId) {
  const panel = document.getElementById('interviewPanel');
  try {
    const list = await api('/students/me/applications/' + appId + '/interviews');
    panel.classList.remove('d-none');
    panel.innerHTML = '<h6>Interview schedule</h6>' + (list.length === 0 ? '<p class="text-muted mb-0">No interviews scheduled yet.</p>'
      : '<table class="table table-sm mb-0"><thead><tr><th>Round</th><th>Stage</th><th>When</th><th>Mode</th><th>Result</th></tr></thead><tbody>'
        + list.map(i => `<tr><td>${esc(i.round)}</td><td>${esc(i.type)}</td><td>${esc(fmtDate(i.scheduledDate))}</td><td>${esc(i.mode || '—')}</td><td>${pill(i.result)}</td></tr>`).join('')
        + '</tbody></table>');
  } catch (err) { showAlert('alert', err.message); }
}

async function withdraw(id) {
  if (!confirm('Withdraw this application?')) return;
  try { await api('/students/me/applications/' + id, 'DELETE'); loadApplications(); }
  catch (err) { showAlert('alert', err.message); }
}

// ---------------- offers ----------------
async function loadOffers() {
  const body = document.getElementById('offersBody');
  try {
    const offers = await api('/students/me/offers');
    body.innerHTML = offers.length === 0 ? '<tr><td colspan="5" class="text-muted">No offers yet.</td></tr>'
      : offers.map(o => `<tr><td>${esc(o.application.internship.role)}</td>
          <td>${esc(o.application.internship.company.companyName)}</td><td>${esc(o.offerDate)}</td><td>${pill(o.status)}</td>
          <td>${o.status === 'EXTENDED' ? `<button class="btn btn-sm btn-success" onclick="respond(${o.id},'ACCEPTED')">Accept</button>
            <button class="btn btn-sm btn-outline-danger" onclick="respond(${o.id},'DECLINED')">Decline</button>` : ''}</td></tr>`).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function respond(id, status) {
  try { await api('/students/me/offers/' + id + '/respond?status=' + status, 'POST'); loadOffers(); }
  catch (err) { showAlert('alert', err.message); }
}

// ---------------- notifications ----------------
async function loadNotifications() {
  const box = document.getElementById('notifList');
  try {
    const list = await api('/notifications/me');
    box.innerHTML = list.length === 0 ? '<p class="text-muted">No notifications.</p>'
      : list.map(n => `<div class="card card-portal p-3 mb-2 ${n.read ? 'opacity-50' : ''}">
          <div class="d-flex justify-content-between"><span>${pill(n.type)} ${esc(n.message)}</span>
          <span class="small text-muted">${esc(fmtDate(n.createdAt))}
          ${n.read ? '' : `<button class="btn btn-sm btn-link" onclick="markRead(${n.id})">Mark read</button>`}</span></div></div>`).join('');
    refreshUnread(list);
  } catch (err) { showAlert('alert', err.message); }
}

async function markRead(id) { try { await api('/notifications/' + id + '/read', 'POST'); loadNotifications(); } catch (e) { showAlert('alert', e.message); } }

function refreshUnread(list) {
  const n = list.filter(x => !x.read).length;
  const el = document.getElementById('unread');
  el.textContent = n; el.classList.toggle('d-none', n === 0);
}

loadProfile();
api('/notifications/me').then(refreshUnread).catch(() => {});

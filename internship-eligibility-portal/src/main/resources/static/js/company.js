requireAuth('COMPANY');
document.getElementById('userEmail').textContent = getEmail();

const internshipsById = {};

setupTabs(tab => {
  if (tab === 'mine') loadInternships();
  if (tab === 'notifications') loadNotifications();
});

document.getElementById('internshipForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const stages = Array.from(document.getElementById('i-stages').selectedOptions).map(o => o.value).join(',');
  try {
    const created = await api('/companies/me/internships', 'POST', {
      role: document.getElementById('i-role').value,
      description: document.getElementById('i-description').value,
      duration: document.getElementById('i-duration').value,
      stipend: num('i-stipend'),
      location: document.getElementById('i-location').value,
      workMode: document.getElementById('i-workmode').value,
      vacancies: num('i-vacancies') ?? 1,
      deadline: document.getElementById('i-deadline').value || null,
      interviewStages: stages,
      minCgpa: num('e-cgpa') ?? 0,
      maxBacklogs: num('e-backlogs') ?? 99,
      eligibleBranches: document.getElementById('e-branches').value,
      requiredSkills: document.getElementById('e-skills').value,
      requiredCertifications: document.getElementById('e-certs').value,
      requiredSemester: num('e-semester'),
      minResumeScore: num('e-resume'),
      minAtrScore: num('e-atr')
    });
    showAlert('alert', created.status === 'PENDING_APPROVAL'
      ? 'Internship submitted — it will go live once faculty approve it.' : 'Internship posted and live.', 'success');
    document.getElementById('internshipForm').reset();
  } catch (err) { showAlert('alert', err.message); }
});

function actionsFor(a, internshipId) {
  const b = [];
  const btn = (label, cls, fn) => `<button class="btn btn-sm btn-outline-${cls} mb-1" onclick="${fn}">${label}</button>`;
  if (a.status === 'FACULTY_SHORTLISTED') b.push(btn('Shortlist', 'primary', `act(${a.id},'shortlist')`));
  if (a.status === 'COMPANY_SHORTLISTED' || a.status === 'INTERVIEW_SCHEDULED') {
    b.push(btn('Schedule', 'primary', `scheduleInterview(${a.id},${internshipId})`));
    b.push(btn('Select', 'success', `act(${a.id},'select')`));
  }
  if (a.status === 'COMPANY_SHORTLISTED' || a.status === 'INTERVIEW_SCHEDULED' || a.status === 'SELECTED') {
    b.push(btn('Interviews', 'secondary', `showInterviews(${a.id})`));
  }
  if (a.status === 'SELECTED') b.push(btn('Extend Offer', 'success', `act(${a.id},'offer')`));
  if (!['SELECTED', 'REJECTED', 'WITHDRAWN'].includes(a.status)) b.push(btn('Reject', 'danger', `rejectApp(${a.id})`));
  b.push(btn('Scores', 'secondary', `assess(${a.id})`));
  return b.join(' ');
}

async function loadInternships() {
  const container = document.getElementById('internshipsList');
  container.innerHTML = '<p class="text-muted">Loading...</p>';
  try {
    const internships = await api('/companies/me/internships');
    if (internships.length === 0) { container.innerHTML = '<p class="text-muted">You have not posted any internships yet.</p>'; return; }
    container.innerHTML = '';
    for (const i of internships) {
      internshipsById[i.id] = i;
      const ranked = await api('/companies/me/internships/' + i.id + '/ranking');
      const c = i.eligibilityCriteria;
      const crit = c ? `Min CGPA ${esc(c.minCgpa)} · Max backlogs ${esc(c.maxBacklogs)} · ${esc(c.eligibleBranches || 'any branch')} · Skills: ${esc(c.requiredSkills || 'none')}` : '';
      const card = document.createElement('div');
      card.className = 'card card-portal p-4 mb-3';
      card.innerHTML = `
        <div class="d-flex justify-content-between flex-wrap">
          <h6>${esc(i.role)} ${pill(i.status)}</h6>
          <span>
            <button class="btn btn-sm btn-outline-secondary" onclick="showEligibleStudents(${i.id})">Eligible students</button>
            <button class="btn btn-sm btn-outline-primary" onclick="editInternship(${i.id})">Edit criteria</button>
            ${i.status === 'CLOSED' ? '' : `<button class="btn btn-sm btn-outline-danger" onclick="closeInternship(${i.id})">Close</button>`}
          </span>
        </div>
        <p class="small text-muted mb-1">${esc(i.location)} · ${esc(i.workMode)} · Stipend ${esc(i.stipend ?? '—')} · Stages: ${esc((i.interviewStages || 'none').replaceAll(',', ' → '))}</p>
        <p class="small text-muted mb-2">${crit}</p>
        <div class="table-responsive"><table class="table table-sm align-middle">
          <thead><tr><th>#</th><th>Student</th><th>CGPA</th><th>Resume match</th><th>ATR</th><th>Score</th><th>Status</th><th></th></tr></thead>
          <tbody>${ranked.length === 0 ? '<tr><td colspan="8" class="text-muted">No applicants yet.</td></tr>' : ranked.map((r, idx) => `
            <tr><td>${idx + 1}</td><td>${esc(r.application.student.name)}</td><td>${esc(r.application.student.cgpa)}</td>
              <td>${r.resumeMatchScore != null ? esc(r.resumeMatchScore.toFixed(0)) + '%' : '—'}</td>
              <td>${r.atrScore != null ? esc(r.atrScore) : '—'}</td><td><b>${esc(r.compositeScore)}</b></td>
              <td>${pill(r.application.status)}</td><td>${actionsFor(r.application, i.id)}</td></tr>`).join('')}
          </tbody></table></div>
        <p class="small text-muted mb-0">Ranking score = 0.6 × resume match + 0.4 × ATR.</p>`;
      container.appendChild(card);
    }
  } catch (err) { showAlert('alert', err.message); }
}

function panel(html) {
  const p = document.getElementById('sidePanel');
  p.innerHTML = html; p.classList.remove('d-none');
  p.scrollIntoView({ behavior: 'smooth' });
}

async function act(appId, action) {
  try {
    await api('/companies/me/applications/' + appId + '/' + action, 'POST');
    showAlert('alert', 'Done: ' + action, 'success');
    loadInternships();
  } catch (err) { showAlert('alert', err.message); }
}

async function rejectApp(appId) {
  const reason = prompt('Reason (optional):') ;
  if (reason === null) return;
  try { await api('/companies/me/applications/' + appId + '/reject?reason=' + encodeURIComponent(reason), 'POST'); loadInternships(); }
  catch (err) { showAlert('alert', err.message); }
}

async function closeInternship(id) {
  if (!confirm('Close this internship to new applications?')) return;
  try { await api('/companies/me/internships/' + id + '/close', 'POST'); loadInternships(); }
  catch (err) { showAlert('alert', err.message); }
}

async function editInternship(id) {
  const c = (internshipsById[id] || {}).eligibilityCriteria || {};
  const cgpa = prompt('Minimum CGPA:', c.minCgpa ?? ''); if (cgpa === null) return;
  const backlogs = prompt('Maximum backlogs:', c.maxBacklogs ?? ''); if (backlogs === null) return;
  const branches = prompt('Eligible branches (comma separated):', c.eligibleBranches ?? ''); if (branches === null) return;
  const skills = prompt('Required skills (comma separated):', c.requiredSkills ?? ''); if (skills === null) return;
  const body = {};
  if (cgpa !== '') body.minCgpa = Number(cgpa);
  if (backlogs !== '') body.maxBacklogs = Number(backlogs);
  body.eligibleBranches = branches; body.requiredSkills = skills;
  try {
    await api('/companies/me/internships/' + id, 'PUT', body);
    showAlert('alert', 'Criteria updated. Students who just became eligible have been notified.', 'success');
    loadInternships();
  } catch (err) { showAlert('alert', err.message); }
}

async function showEligibleStudents(id) {
  try {
    const list = await api('/companies/me/internships/' + id + '/eligible-students');
    panel('<h6>Students who currently satisfy this internship\'s criteria</h6>' + (list.length === 0
      ? '<p class="text-muted mb-0">No students match right now.</p>'
      : '<table class="table table-sm mb-0"><thead><tr><th>Name</th><th>Branch</th><th>CGPA</th><th>Backlogs</th><th>Verified</th></tr></thead><tbody>'
        + list.map(s => `<tr><td>${esc(s.name)}</td><td>${esc(s.department)}</td><td>${esc(s.cgpa)}</td><td>${esc(s.backlogs)}</td><td>${s.verified ? 'Yes' : 'No'}</td></tr>`).join('') + '</tbody></table>'));
  } catch (err) { showAlert('alert', err.message); }
}

async function scheduleInterview(appId, internshipId) {
  const stages = ((internshipsById[internshipId] || {}).interviewStages || '').split(',').filter(Boolean);
  const type = prompt('Interview stage' + (stages.length ? ' (' + stages.join(', ') + ')' : ' (APTITUDE_TEST, CODING_TEST, TECHNICAL, HR, GROUP_DISCUSSION)'), stages[0] || 'TECHNICAL');
  if (!type) return;
  const when = prompt('Date & time (YYYY-MM-DD HH:MM):', '');
  if (!when || !/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}$/.test(when.trim())) { showAlert('alert', 'Please enter the date as YYYY-MM-DD HH:MM'); return; }
  const mode = prompt('Mode (ONLINE / OFFLINE):', 'ONLINE') || 'ONLINE';
  try {
    await api('/interviews/schedule', 'POST', {
      applicationId: appId, type: type.trim().toUpperCase(),
      scheduledDate: when.trim().replace(' ', 'T') + ':00', mode: mode.trim().toUpperCase()
    });
    showAlert('alert', 'Interview scheduled; the student was notified.', 'success');
    loadInternships();
  } catch (err) { showAlert('alert', err.message); }
}

async function showInterviews(appId) {
  try {
    const list = await api('/interviews/application/' + appId);
    panel('<h6>Interviews for this application</h6>' + (list.length === 0 ? '<p class="text-muted mb-0">None scheduled.</p>'
      : '<table class="table table-sm mb-0"><thead><tr><th>Round</th><th>Stage</th><th>When</th><th>Mode</th><th>Result</th><th></th></tr></thead><tbody>'
        + list.map(i => `<tr><td>${esc(i.round)}</td><td>${esc(i.type)}</td><td>${esc(fmtDate(i.scheduledDate))}</td><td>${esc(i.mode || '—')}</td><td>${pill(i.result)}</td>
          <td>${i.result === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="recordResult(${i.id},'PASS',${appId})">Pass</button>
             <button class="btn btn-sm btn-danger" onclick="recordResult(${i.id},'FAIL',${appId})">Fail</button>` : esc(i.remarks || '')}</td></tr>`).join('') + '</tbody></table>'));
  } catch (err) { showAlert('alert', err.message); }
}

async function recordResult(interviewId, result, appId) {
  const remarks = prompt('Remarks (optional):') || '';
  try {
    await api('/interviews/' + interviewId + '/result', 'POST', { result, remarks });
    await loadInternships();
    showInterviews(appId);
  } catch (err) { showAlert('alert', err.message); }
}

async function assess(appId) {
  const atr = prompt('ATR score 0-100 (leave blank to skip):'); if (atr === null) return;
  const apt = prompt('Aptitude score 0-100 (blank to skip):'); if (apt === null) return;
  const cod = prompt('Coding score 0-100 (blank to skip):'); if (cod === null) return;
  const body = { applicationId: appId };
  if (atr !== '') body.atrScore = Number(atr);
  if (apt !== '') body.aptitudeScore = Number(apt);
  if (cod !== '') body.codingScore = Number(cod);
  try { await api('/interviews/assessment', 'POST', body); showAlert('alert', 'Scores saved.', 'success'); loadInternships(); }
  catch (err) { showAlert('alert', err.message); }
}

async function loadNotifications() {
  try {
    const list = await api('/notifications/me');
    document.getElementById('notifList').innerHTML = list.length === 0 ? '<p class="text-muted">No notifications.</p>'
      : list.map(n => `<div class="card card-portal p-3 mb-2"><div class="d-flex justify-content-between"><span>${pill(n.type)} ${esc(n.message)}</span>
          <span class="small text-muted">${esc(fmtDate(n.createdAt))}</span></div></div>`).join('');
  } catch (err) { showAlert('alert', err.message); }
}

loadInternships();

requireAuth('FACULTY');
document.getElementById('userEmail').textContent = getEmail();

setupTabs(tab => {
  if (tab === 'students') loadStudents();
  if (tab === 'applications') loadApplications();
  if (tab === 'approvals') loadApprovals();
  if (tab === 'reports') loadReports('reportCards');
});

async function loadStudents() {
  const body = document.getElementById('studentsBody');
  try {
    const students = await api('/faculty/students');
    body.innerHTML = students.length === 0 ? '<tr><td colspan="8" class="text-muted">No students registered yet.</td></tr>'
      : students.map(s => `<tr>
          <td>${esc(s.name)}</td><td>${esc(s.prn || '—')}</td><td>${esc(s.department || '—')}</td><td>${esc(s.semester ?? '—')}</td>
          <td>${esc(s.cgpa ?? '—')}</td><td>${esc(s.backlogs ?? '—')}</td>
          <td>${s.verified ? '<span class="badge badge-eligible">VERIFIED</span>' : '<span class="badge bg-warning text-dark">PENDING</span>'}</td>
          <td class="text-nowrap">
            <button class="btn btn-sm btn-outline-secondary" onclick="showDocs(${s.id})">Documents</button>
            <button class="btn btn-sm btn-outline-success" onclick="verify(${s.id},'VERIFIED')">Verify</button>
            <button class="btn btn-sm btn-outline-danger" onclick="verify(${s.id},'REJECTED')">Reject</button>
          </td></tr>`).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function verify(studentId, status) {
  const remarks = status === 'REJECTED' ? (prompt('Reason / remarks for the student:') || '') : '';
  try {
    await api('/faculty/students/' + studentId + '/verify?type=PROFILE&status=' + status + '&remarks=' + encodeURIComponent(remarks), 'POST');
    showAlert('alert', 'Profile marked ' + status.toLowerCase() + '.', 'success');
    loadStudents();
  } catch (err) { showAlert('alert', err.message); }
}

async function showDocs(studentId) {
  const panel = document.getElementById('docPanel');
  try {
    const docs = await api('/faculty/students/' + studentId + '/documents');
    panel.classList.remove('d-none');
    panel.innerHTML = '<h6>Student documents</h6>' + (docs.length === 0 ? '<p class="text-muted mb-0">No documents uploaded.</p>'
      : '<table class="table table-sm mb-0"><tbody>' + docs.map(d => `<tr><td>${esc(d.documentType)}</td><td>${pill(d.verificationStatus)}</td>
          <td><button class="btn btn-sm btn-outline-secondary" onclick="openFile('/faculty/documents/${d.id}/file')">View</button>
          <button class="btn btn-sm btn-outline-success" onclick="verifyDoc(${d.id},'VERIFIED',${studentId})">Verify</button>
          <button class="btn btn-sm btn-outline-danger" onclick="verifyDoc(${d.id},'REJECTED',${studentId})">Reject</button></td></tr>`).join('') + '</tbody></table>');
    panel.scrollIntoView({ behavior: 'smooth' });
  } catch (err) { showAlert('alert', err.message); }
}

async function verifyDoc(docId, status, studentId) {
  try { await api('/faculty/documents/' + docId + '/verify?status=' + status, 'POST'); showDocs(studentId); }
  catch (err) { showAlert('alert', err.message); }
}

async function loadApplications() {
  const body = document.getElementById('applicationsBody');
  try {
    const apps = await api('/faculty/applications');
    body.innerHTML = apps.length === 0 ? '<tr><td colspan="5" class="text-muted">No applications yet.</td></tr>'
      : apps.map(a => {
        const open = a.status === 'APPLIED' || a.status === 'RESUME_SCORED';
        const closed = ['SELECTED', 'REJECTED', 'WITHDRAWN'].includes(a.status);
        return `<tr><td>${esc(a.student.name)}</td><td>${esc(a.internship.role)} <span class="text-muted small">@ ${esc(a.internship.company.companyName)}</span></td>
          <td>${a.resumeMatchScore != null ? esc(a.resumeMatchScore.toFixed(0)) + '%' : '—'}</td><td>${pill(a.status)}</td>
          <td class="text-nowrap">
            <button class="btn btn-sm btn-outline-secondary" onclick="setAtr(${a.id})">ATR score</button>
            ${open ? `<button class="btn btn-sm btn-outline-primary" onclick="shortlist(${a.id})">Shortlist</button>` : ''}
            ${closed ? '' : `<button class="btn btn-sm btn-outline-danger" onclick="rejectApp(${a.id})">Reject</button>`}
          </td></tr>`;
      }).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function setAtr(appId) {
  const atr = prompt('ATR / assessment score (0-100):');
  if (atr === null || atr === '') return;
  try { await api('/interviews/assessment', 'POST', { applicationId: appId, atrScore: Number(atr) }); showAlert('alert', 'ATR score saved.', 'success'); }
  catch (err) { showAlert('alert', err.message); }
}

async function shortlist(appId) {
  try { await api('/faculty/applications/' + appId + '/shortlist', 'POST'); showAlert('alert', 'Candidate shortlisted by faculty.', 'success'); loadApplications(); }
  catch (err) { showAlert('alert', err.message); }
}

async function rejectApp(appId) {
  const reason = prompt('Reason (optional):');
  if (reason === null) return;
  try { await api('/faculty/applications/' + appId + '/reject?reason=' + encodeURIComponent(reason), 'POST'); loadApplications(); }
  catch (err) { showAlert('alert', err.message); }
}

async function loadApprovals() {
  const box = document.getElementById('approvalsList');
  try {
    const list = await api('/faculty/internships/pending');
    box.innerHTML = list.length === 0 ? '<p class="text-muted">Nothing waiting for approval.</p>'
      : list.map(i => { const c = i.eligibilityCriteria; return `<div class="card card-portal p-3 mb-2">
          <div class="d-flex justify-content-between flex-wrap">
            <div><b>${esc(i.role)}</b> <span class="text-muted">@ ${esc(i.company.companyName)}</span>
              <div class="small text-muted">${esc(i.location)} · ${esc(i.workMode)} · Stipend ${esc(i.stipend ?? '—')} · Stages: ${esc((i.interviewStages || 'none').replaceAll(',', ' → '))}</div>
              <div class="small">${c ? `Min CGPA ${esc(c.minCgpa)} · Max backlogs ${esc(c.maxBacklogs)} · ${esc(c.eligibleBranches || 'any branch')} · Skills: ${esc(c.requiredSkills || 'none')}` : ''}</div></div>
            <div><button class="btn btn-sm btn-success" onclick="decide(${i.id},'approve')">Approve</button>
              <button class="btn btn-sm btn-outline-danger" onclick="decide(${i.id},'decline')">Decline</button></div>
          </div></div>`; }).join('');
  } catch (err) { showAlert('alert', err.message); }
}

async function decide(id, action) {
  try { await api('/faculty/internships/' + id + '/' + action, 'POST'); loadApprovals(); }
  catch (err) { showAlert('alert', err.message); }
}

loadStudents();

function statCard(label, value, cols = 3) {
  return `<div class="col-md-${cols} col-6"><div class="card card-portal p-3 text-center">
    <div class="small text-muted">${esc(label)}</div><div class="fs-3 fw-bold">${esc(value)}</div></div></div>`;
}
function mapTable(title, map) {
  const rows = Object.entries(map || {});
  return `<div class="col-md-4"><div class="card card-portal p-3 h-100"><h6>${esc(title)}</h6>` +
    (rows.length === 0 ? '<p class="text-muted small mb-0">No data yet.</p>'
      : '<table class="table table-sm mb-0"><tbody>' + rows.map(([k, v]) => `<tr><td>${esc(k.replaceAll('_', ' '))}</td><td class="text-end fw-bold">${esc(v)}</td></tr>`).join('') + '</tbody></table>')
    + '</div></div>';
}
async function loadReports(containerId) {
  const container = document.getElementById(containerId);
  container.innerHTML = '<p class="text-muted">Loading...</p>';
  try {
    const r = await api('/reports/summary');
    container.innerHTML =
      statCard('Total Students', r.totalStudents) + statCard('Verified Students', r.verifiedStudents) +
      statCard('Eligible Students', r.eligibleStudents) + statCard('Selected Students', r.selectedStudents) +
      statCard('Total Internships', r.totalInternships) + statCard('Open Internships', r.openInternships) +
      statCard('Applications', r.totalApplications) + statCard('Shortlisted', r.shortlistedApplications) +
      '<div class="col-12"><hr></div>' +
      mapTable('Applications by Status', r.applicationsByStatus) +
      mapTable('Internship-wise', r.internshipWise) +
      mapTable('Company-wise', r.companyWise) +
      mapTable('Department-wise', r.departmentWise);
  } catch (err) { showAlert('alert', err.message); }
}

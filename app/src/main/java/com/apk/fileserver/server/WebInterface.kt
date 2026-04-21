package com.apk.fileserver.server

object WebInterface {

    fun getMainPageHtml(): String {
        return buildMainPage()
    }

    fun getLoginPageHtml(): String {
        return buildLoginPage()
    }

    private fun buildMainPage(): String {
        val css = getMainCss()
        val html = getMainHtml()
        val js = getMainJs()
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>LocalShare - File Browser</title>
<style>
$css
</style>
</head>
<body>
$html
<script>
$js
</script>
</body>
</html>"""
    }

    private fun getMainCss(): String {
        return """
:root {
  --bg: #0f172a;
  --surface: #1e293b;
  --surface2: #334155;
  --primary: #3b82f6;
  --primary-hover: #2563eb;
  --text: #f1f5f9;
  --text-muted: #94a3b8;
  --border: #334155;
  --success: #22c55e;
  --danger: #ef4444;
  --warning: #f59e0b;
  --radius: 12px;
  --shadow: 0 4px 24px rgba(0,0,0,0.4);
}
* { margin:0; padding:0; box-sizing:border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg);
  color: var(--text);
  min-height: 100vh;
}
.topbar {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
.logo { font-size: 20px; font-weight: 700; color: var(--primary); }
.topbar-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.search-wrap { position: relative; display: flex; align-items: center; }
.search-input {
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  padding: 7px 12px 7px 32px;
  font-size: 14px;
  width: 200px;
  outline: none;
}
.search-input:focus { border-color: var(--primary); }
.search-icon { position: absolute; left: 9px; color: var(--text-muted); font-size: 15px; }
.btn {
  padding: 7px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s;
  white-space: nowrap;
}
.btn-primary { background: var(--primary); color: white; }
.btn-primary:hover { background: var(--primary-hover); }
.btn-success { background: var(--success); color: white; }
.btn-success:hover { opacity: 0.9; }
.btn-warning { background: var(--warning); color: #000; }
.btn-warning:hover { opacity: 0.9; }
.btn-ghost { background: transparent; color: var(--text-muted); border: 1px solid var(--border); }
.btn-ghost:hover { background: var(--surface2); color: var(--text); }
.btn-danger { background: transparent; color: var(--danger); border: 1px solid var(--danger); }
.btn-danger:hover { background: var(--danger); color: white; }
.btn-sm { padding: 4px 10px; font-size: 12px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.breadcrumb {
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-muted);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.breadcrumb a { color: var(--primary); text-decoration: none; }
.breadcrumb a:hover { opacity: 0.8; }
.storage-bar { padding: 10px 20px; display: flex; gap: 10px; flex-wrap: wrap; }
.storage-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 10px 14px;
  cursor: pointer;
  flex: 1;
  min-width: 160px;
  transition: border-color 0.2s;
}
.storage-card:hover, .storage-card.active { border-color: var(--primary); }
.storage-name { font-size: 12px; font-weight: 600; margin-bottom: 5px; }
.storage-progress { height: 3px; background: var(--surface2); border-radius: 2px; overflow: hidden; margin-bottom: 3px; }
.storage-fill { height: 100%; background: var(--primary); border-radius: 2px; }
.storage-info { font-size: 10px; color: var(--text-muted); }
.main { padding: 12px 20px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.toolbar-right { display: flex; gap: 6px; align-items: center; }
.item-count { font-size: 12px; color: var(--text-muted); }

/* ── SELECTION BAR ── */
.selection-bar {
  display: none;
  background: var(--primary);
  padding: 8px 16px;
  border-radius: 10px;
  margin-bottom: 10px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.selection-bar.show { display: flex; }
.selection-info { font-size: 13px; font-weight: 600; color: white; }
.selection-actions { display: flex; gap: 8px; }
.btn-white {
  background: white;
  color: var(--primary);
  border: none;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.btn-white-outline {
  background: transparent;
  color: white;
  border: 1px solid rgba(255,255,255,0.5);
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

/* ── FILE TABLE ── */
.file-table {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}
.file-table-header {
  display: grid;
  grid-template-columns: 40px 28px 1fr 90px 130px 130px;
  padding: 9px 14px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border);
  background: var(--surface2);
  align-items: center;
}
.file-row {
  display: grid;
  grid-template-columns: 40px 28px 1fr 90px 130px 130px;
  padding: 9px 14px;
  align-items: center;
  border-bottom: 1px solid var(--border);
  transition: background 0.1s;
  cursor: pointer;
}
.file-row:last-child { border-bottom: none; }
.file-row:hover { background: var(--surface2); }
.file-row.selected { background: rgba(59,130,246,0.15); }
.file-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--primary);
}
.file-icon { font-size: 18px; }
.file-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 10px;
}
.file-name a { color: var(--text); text-decoration: none; }
.file-name a:hover { color: var(--primary); }
.file-size { font-size: 12px; color: var(--text-muted); }
.file-date { font-size: 11px; color: var(--text-muted); }
.file-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.file-row:hover .file-actions { opacity: 1; }

/* ── UPLOAD ZONE ── */
.upload-zone {
  border: 2px dashed var(--border);
  border-radius: var(--radius);
  padding: 28px;
  text-align: center;
  margin-top: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.upload-zone:hover, .upload-zone.drag-over {
  border-color: var(--primary);
  background: rgba(59,130,246,0.06);
}
.upload-icon { font-size: 32px; margin-bottom: 6px; }
.upload-text { font-size: 14px; font-weight: 500; margin-bottom: 3px; }
.upload-subtext { font-size: 12px; color: var(--text-muted); }
.upload-progress { margin-top: 10px; display: none; }
.progress-item {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 9px 12px;
  margin-bottom: 6px;
}
.progress-name {
  font-size: 12px;
  margin-bottom: 5px;
  display: flex;
  justify-content: space-between;
}
.progress-bar { height: 3px; background: var(--surface2); border-radius: 2px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--primary); border-radius: 2px; width: 0%; transition: width 0.1s; }

/* ── MODAL ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: none;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.modal-overlay.show { display: flex; }
.modal {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 22px;
  width: 340px;
  box-shadow: var(--shadow);
}
.modal h3 { margin-bottom: 14px; font-size: 15px; }
.modal-input {
  width: 100%;
  padding: 9px 12px;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  outline: none;
  margin-bottom: 14px;
}
.modal-input:focus { border-color: var(--primary); }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }

/* ── TOAST ── */
.toast-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 300;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.toast {
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  min-width: 180px;
  max-width: 300px;
  box-shadow: var(--shadow);
  animation: slideIn 0.25s ease;
}
.toast.success { border-left: 3px solid var(--success); }
.toast.error   { border-left: 3px solid var(--danger); }
.toast.info    { border-left: 3px solid var(--primary); }
@keyframes slideIn {
  from { transform: translateX(110%); opacity: 0; }
  to   { transform: translateX(0);    opacity: 1; }
}
.empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); }
.empty-icon { font-size: 40px; margin-bottom: 10px; }

/* ── SPEED INDICATOR ── */
.speed-badge {
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 11px;
  color: var(--success);
  font-family: monospace;
}

@media (max-width: 700px) {
  .file-table-header,
  .file-row { grid-template-columns: 36px 24px 1fr 70px 100px; }
  .file-date { display: none; }
  .search-input { width: 130px; }
}
@media (max-width: 500px) {
  .file-table-header,
  .file-row { grid-template-columns: 36px 24px 1fr 70px; }
  .file-actions { display: none; }
}"""
    }

    private fun getMainHtml(): String {
        return """
<div class="topbar">
  <div class="logo">&#128193; LocalShare</div>
  <div class="topbar-actions">
    <div class="search-wrap">
      <span class="search-icon">&#128269;</span>
      <input class="search-input" id="searchInput"
             placeholder="Search..."
             oninput="handleSearch(this.value)">
    </div>
    <button class="btn btn-primary" onclick="showUploadZone()">&#11014; Upload</button>
    <button class="btn btn-ghost" onclick="showMkdirModal()">&#128193;+ New</button>
  </div>
</div>

<div class="storage-bar" id="storageBar"></div>
<div class="breadcrumb" id="breadcrumb"></div>

<div class="main">

  <!-- TOOLBAR -->
  <div class="toolbar">
    <div class="toolbar-left">
      <button class="btn btn-ghost btn-sm" onclick="goUp()">&#8592; Back</button>
      <button class="btn btn-ghost btn-sm" onclick="refresh()">&#8635; Refresh</button>
      <button class="btn btn-ghost btn-sm" onclick="toggleSelectAll()" id="selectAllBtn">
        &#9745; Select All
      </button>
      <span class="item-count" id="itemCount"></span>
    </div>
    <div class="toolbar-right">
      <span class="speed-badge" id="speedBadge" style="display:none"></span>
      <button class="btn btn-warning btn-sm" id="zipFolderBtn"
              onclick="downloadCurrentFolderAsZip()" style="display:none">
        &#128230; Download Folder
      </button>
    </div>
  </div>

  <!-- SELECTION BAR -->
  <div class="selection-bar" id="selectionBar">
    <span class="selection-info" id="selectionInfo">0 selected</span>
    <div class="selection-actions">
      <button class="btn-white" onclick="downloadSelected()">
        &#11015; Download ZIP
      </button>
      <button class="btn-white-outline" onclick="deleteSelected()">
        &#128465; Delete
      </button>
      <button class="btn-white-outline" onclick="clearSelection()">
        &#10005; Cancel
      </button>
    </div>
  </div>

  <!-- FILE TABLE -->
  <div class="file-table" id="fileTable">
    <div class="file-table-header">
      <div>
        <input type="checkbox" class="file-checkbox"
               id="headerCheckbox"
               onchange="handleHeaderCheckbox(this.checked)">
      </div>
      <div></div>
      <div>Name</div>
      <div>Size</div>
      <div>Modified</div>
      <div>Actions</div>
    </div>
    <div id="fileList"></div>
  </div>

  <!-- UPLOAD ZONE -->
  <div class="upload-zone" id="uploadZone"
       style="display:none"
       ondrop="handleDrop(event)"
       ondragover="handleDragOver(event)"
       ondragleave="handleDragLeave(event)"
       onclick="document.getElementById('fileInput').click()">
    <div class="upload-icon">&#128228;</div>
    <div class="upload-text">Drop files here or click to select</div>
    <div class="upload-subtext">Multiple files supported &#8226; Upload to current folder</div>
    <input type="file" id="fileInput" multiple style="display:none"
           onchange="handleFileSelect(this.files)">
  </div>

  <div class="upload-progress" id="uploadProgress"></div>
</div>

<!-- MKDIR MODAL -->
<div class="modal-overlay" id="mkdirModal">
  <div class="modal">
    <h3>&#128193; Create New Folder</h3>
    <input class="modal-input" id="folderNameInput"
           placeholder="Folder name"
           onkeydown="if(event.key==='Enter') createFolder()">
    <div class="modal-actions">
      <button class="btn btn-ghost" onclick="hideMkdirModal()">Cancel</button>
      <button class="btn btn-primary" onclick="createFolder()">Create</button>
    </div>
  </div>
</div>

<!-- RENAME MODAL -->
<div class="modal-overlay" id="renameModal">
  <div class="modal">
    <h3>&#9999; Rename</h3>
    <input class="modal-input" id="renameInput"
           placeholder="New name"
           onkeydown="if(event.key==='Enter') doRename()">
    <div class="modal-actions">
      <button class="btn btn-ghost" onclick="hideRenameModal()">Cancel</button>
      <button class="btn btn-primary" onclick="doRename()">Rename</button>
    </div>
  </div>
</div>

<div class="toast-container" id="toastContainer"></div>"""
    }

    private fun getMainJs(): String {
        return """
var currentPath = '';
var currentItems = [];
var renameTarget = '';
var searchTimeout = null;
var selectedPaths = [];
var uploadStartTime = 0;
var uploadedBytes = 0;

document.addEventListener('DOMContentLoaded', function() {
  loadStorage();
});

// ═══════════════════════════════════════
//          STORAGE
// ═══════════════════════════════════════

function loadStorage() {
  fetch('/api/storage')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      renderStorage(data.roots);
      if (data.roots.length > 0) navigateTo(data.roots[0].path);
    })
    .catch(function() { showToast('Failed to load storage', 'error'); });
}

function renderStorage(roots) {
  var bar = document.getElementById('storageBar');
  var html = '';
  for (var i = 0; i < roots.length; i++) {
    var r = roots[i];
    html += '<div class="storage-card ' + (i===0?'active':'') + '"' +
            ' onclick="navigateTo(\'' + escJs(r.path) + '\')">' +
            '<div class="storage-name">' + r.icon + ' ' + escHtml(r.name) + '</div>' +
            '<div class="storage-progress">' +
            '<div class="storage-fill" style="width:' + r.usedPercent + '%"></div></div>' +
            '<div class="storage-info">' + r.usedFormatted + ' / ' + r.totalFormatted + '</div>' +
            '</div>';
  }
  bar.innerHTML = html;
}

// ═══════════════════════════════════════
//          FILE LISTING
// ═══════════════════════════════════════

function navigateTo(path) {
  currentPath = path;
  clearSelection();
  fetch('/api/list?path=' + encodeURIComponent(path))
    .then(function(r) { return r.json(); })
    .then(function(data) {
      currentItems = data.items;
      renderBreadcrumb(data.breadcrumbs);
      renderFiles(data.items);
      document.getElementById('itemCount').textContent = data.totalItems + ' items';
      document.getElementById('zipFolderBtn').style.display = 'inline-flex';
    })
    .catch(function() { showToast('Failed to load directory', 'error'); });
}

function renderBreadcrumb(crumbs) {
  var bc = document.getElementById('breadcrumb');
  var html = '';
  for (var i = 0; i < crumbs.length; i++) {
    var c = crumbs[i];
    var isLast = i === crumbs.length - 1;
    if (isLast) {
      html += '<span>' + escHtml(c.name) + '</span>';
    } else {
      html += '<a href="#" onclick="navigateTo(\'' + escJs(c.path) + '\');return false">' +
              escHtml(c.name) + '</a><span style="color:#475569"> &#8250; </span>';
    }
  }
  bc.innerHTML = html;
}

function renderFiles(items) {
  var list = document.getElementById('fileList');
  if (!items || items.length === 0) {
    list.innerHTML = '<div class="empty-state"><div class="empty-icon">&#128237;</div><div>This folder is empty</div></div>';
    return;
  }

  var html = '';
  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var isChecked = selectedPaths.indexOf(item.path) !== -1;

    var nameCell = '';
    if (item.isDirectory) {
      nameCell = '<a href="#" onclick="navigateTo(\'' + escJs(item.path) + '\');return false">' +
                 escHtml(item.name) + '</a>';
    } else {
      nameCell = '<a href="/files' + encodeURIComponent(item.path) +
                 '" target="_blank">' + escHtml(item.name) + '</a>';
    }

    var downloadBtn = '';
    if (!item.isDirectory) {
      downloadBtn = '<a href="/files' + encodeURIComponent(item.path) +
                    '" download="' + escHtml(item.name) + '">' +
                    '<button class="btn btn-ghost btn-sm" onclick="event.stopPropagation()">&#11015;</button></a>';
    } else {
      downloadBtn = '<button class="btn btn-ghost btn-sm"' +
                    ' onclick="event.stopPropagation();downloadFolderAsZip(\'' +
                    escJs(item.path) + '\',\'' + escJs(item.name) + '\')">' +
                    '&#128230;</button>';
    }

    html += '<div class="file-row' + (isChecked ? ' selected' : '') + '"' +
            ' onclick="handleRowClick(event,\'' + escJs(item.path) + '\')">' +
            '<div onclick="event.stopPropagation()">' +
            '<input type="checkbox" class="file-checkbox item-checkbox"' +
            ' data-path="' + escHtml(item.path) + '"' +
            (isChecked ? ' checked' : '') +
            ' onchange="handleCheckbox(this)">' +
            '</div>' +
            '<div class="file-icon">' + item.icon + '</div>' +
            '<div class="file-name">' + nameCell + '</div>' +
            '<div class="file-size">' + (item.isDirectory ? '&mdash;' : item.sizeFormatted) + '</div>' +
            '<div class="file-date">' + item.dateFormatted + '</div>' +
            '<div class="file-actions">' +
            downloadBtn +
            '<button class="btn btn-ghost btn-sm"' +
            ' onclick="event.stopPropagation();showRenameModal(\'' +
            escJs(item.path) + '\',\'' + escJs(item.name) + '\')">&#9999;</button>' +
            '<button class="btn btn-danger btn-sm"' +
            ' onclick="event.stopPropagation();deleteItem(\'' +
            escJs(item.path) + '\',\'' + escJs(item.name) + '\')">&#128465;</button>' +
            '</div></div>';
  }
  list.innerHTML = html;
}

// ═══════════════════════════════════════
//          SELECTION
// ═══════════════════════════════════════

function handleRowClick(event, path) {
  if (event.target.tagName === 'A' ||
      event.target.tagName === 'BUTTON' ||
      event.target.tagName === 'INPUT') return;
  toggleSelection(path);
}

function handleCheckbox(checkbox) {
  var path = checkbox.getAttribute('data-path');
  if (checkbox.checked) {
    addToSelection(path);
  } else {
    removeFromSelection(path);
  }
}

function handleHeaderCheckbox(checked) {
  if (checked) {
    selectAll();
  } else {
    clearSelection();
  }
}

function toggleSelection(path) {
  var idx = selectedPaths.indexOf(path);
  if (idx === -1) {
    addToSelection(path);
  } else {
    removeFromSelection(path);
  }
}

function addToSelection(path) {
  if (selectedPaths.indexOf(path) === -1) {
    selectedPaths.push(path);
  }
  updateSelectionUI();
}

function removeFromSelection(path) {
  var idx = selectedPaths.indexOf(path);
  if (idx !== -1) selectedPaths.splice(idx, 1);
  updateSelectionUI();
}

function selectAll() {
  selectedPaths = [];
  for (var i = 0; i < currentItems.length; i++) {
    selectedPaths.push(currentItems[i].path);
  }
  updateSelectionUI();
  renderFiles(currentItems);
}

function clearSelection() {
  selectedPaths = [];
  updateSelectionUI();
  var checkboxes = document.querySelectorAll('.item-checkbox');
  for (var i = 0; i < checkboxes.length; i++) {
    checkboxes[i].checked = false;
  }
  var rows = document.querySelectorAll('.file-row');
  for (var i = 0; i < rows.length; i++) {
    rows[i].classList.remove('selected');
  }
  var hdr = document.getElementById('headerCheckbox');
  if (hdr) hdr.checked = false;
}

function toggleSelectAll() {
  if (selectedPaths.length === currentItems.length && currentItems.length > 0) {
    clearSelection();
  } else {
    selectAll();
  }
}

function updateSelectionUI() {
  var count = selectedPaths.length;
  var bar = document.getElementById('selectionBar');
  var info = document.getElementById('selectionInfo');
  var hdr = document.getElementById('headerCheckbox');

  if (count > 0) {
    bar.classList.add('show');
    info.textContent = count + ' item' + (count !== 1 ? 's' : '') + ' selected';
  } else {
    bar.classList.remove('show');
  }

  if (hdr) {
    hdr.checked = count > 0 && count === currentItems.length;
    hdr.indeterminate = count > 0 && count < currentItems.length;
  }

  // Update row highlighting
  var rows = document.querySelectorAll('.file-row');
  var checkboxes = document.querySelectorAll('.item-checkbox');
  for (var i = 0; i < checkboxes.length; i++) {
    var path = checkboxes[i].getAttribute('data-path');
    var isSelected = selectedPaths.indexOf(path) !== -1;
    checkboxes[i].checked = isSelected;
    if (rows[i]) {
      if (isSelected) rows[i].classList.add('selected');
      else rows[i].classList.remove('selected');
    }
  }
}

// ═══════════════════════════════════════
//     DOWNLOAD (PIPE STREAMING ZIP)
// ═══════════════════════════════════════

function downloadSelected() {
  if (selectedPaths.length === 0) {
    showToast('No files selected', 'error');
    return;
  }

  // Single file → direct download
  if (selectedPaths.length === 1) {
    var singlePath = selectedPaths[0];
    var singleItem = null;
    for (var i = 0; i < currentItems.length; i++) {
      if (currentItems[i].path === singlePath) {
        singleItem = currentItems[i];
        break;
      }
    }
    if (singleItem && !singleItem.isDirectory) {
      triggerDirectDownload(
        '/files' + encodeURIComponent(singleItem.path),
        singleItem.name
      );
      showToast('Downloading: ' + singleItem.name, 'success');
      clearSelection();
      return;
    }
  }

  // Multiple/folder → streaming ZIP
  // Use iframe trick for chunked response
  // This works because server streams directly - no size limit
  showToast('Starting ZIP download... (streaming)', 'info');
  setBadge('Streaming...');

  var form = document.createElement('form');
  form.method = 'POST';
  form.action = '/api/zip';
  // Target hidden iframe to avoid page navigation
  form.target = 'zipDownloadFrame';

  var input = document.createElement('input');
  input.type  = 'hidden';
  input.name  = 'paths';
  input.value = JSON.stringify(selectedPaths);
  form.appendChild(input);

  // Create hidden iframe for download
  var iframe = document.getElementById('zipDownloadFrame');
  if (!iframe) {
    iframe = document.createElement('iframe');
    iframe.name  = 'zipDownloadFrame';
    iframe.id    = 'zipDownloadFrame';
    iframe.style.display = 'none';
    document.body.appendChild(iframe);
  }

  document.body.appendChild(form);
  form.submit();
  document.body.removeChild(form);

  setTimeout(function() {
    setBadge('');
    showToast('ZIP download started!', 'success');
    clearSelection();
  }, 2000);
}

function downloadFolderAsZip(path, name) {
  showToast('Starting folder ZIP: ' + name, 'info');
  setBadge('Streaming...');

  // Direct GET link works for chunked streaming
  triggerDirectDownload(
    '/api/zipfolder?path=' + encodeURIComponent(path),
    name + '.zip'
  );

  setTimeout(function() {
    setBadge('');
    showToast('Folder ZIP started: ' + name, 'success');
  }, 2000);
}

function downloadCurrentFolderAsZip() {
  var parts = currentPath.split('/').filter(function(s) {
    return s.length > 0;
  });
  var folderName = parts.length > 0
    ? parts[parts.length - 1]
    : 'folder';
  downloadFolderAsZip(currentPath, folderName);
}

function triggerDirectDownload(url, fileName) {
  var a = document.createElement('a');
  a.style.display = 'none';
  a.href     = url;
  a.download = fileName || 'download';
  document.body.appendChild(a);
  a.click();
  setTimeout(function() {
    document.body.removeChild(a);
  }, 2000);
}

function saveBlobAsFile(blob, fileName) {
  var url = window.URL.createObjectURL(blob);
  triggerDirectDownload(url, fileName);
  setTimeout(function() {
    window.URL.revokeObjectURL(url);
  }, 5000);
}

function setBadge(text) {
  var badge = document.getElementById('speedBadge');
  if (!badge) return;
  if (text) {
    badge.style.display = 'inline-block';
    badge.textContent   = text;
  } else {
    badge.style.display = 'none';
    badge.textContent   = '';
  }
}

// ═══════════════════════════════════════
//          DELETE SELECTED
// ═══════════════════════════════════════

function deleteSelected() {
  if (selectedPaths.length === 0) return;
  var count = selectedPaths.length;
  if (!confirm('Delete ' + count + ' item(s)? This cannot be undone.')) return;

  var promises = selectedPaths.map(function(path) {
    return fetch('/api/delete', {
      method: 'POST',
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      body: 'path=' + encodeURIComponent(path)
    }).then(function(r) { return r.json(); });
  });

  Promise.all(promises).then(function(results) {
    var failed = results.filter(function(r) { return !r.success; }).length;
    clearSelection();
    refresh();
    if (failed > 0) {
      showToast(failed + ' item(s) failed to delete', 'error');
    } else {
      showToast(count + ' item(s) deleted!', 'success');
    }
  });
}

// ═══════════════════════════════════════
//          NAVIGATION
// ═══════════════════════════════════════

function goUp() {
  if (!currentPath) return;
  var parent = currentPath.substring(0, currentPath.lastIndexOf('/'));
  if (parent) navigateTo(parent);
}

function refresh() {
  if (currentPath) navigateTo(currentPath);
}

// ═══════════════════════════════════════
//          SEARCH
// ═══════════════════════════════════════

function handleSearch(query) {
  clearTimeout(searchTimeout);
  if (query.length < 2) {
    if (currentPath) navigateTo(currentPath);
    return;
  }
  searchTimeout = setTimeout(function() { doSearch(query); }, 350);
}

function doSearch(query) {
  fetch('/api/search?q=' + encodeURIComponent(query) +
        '&path=' + encodeURIComponent(currentPath))
    .then(function(r) { return r.json(); })
    .then(function(data) {
      renderFiles(data.results);
      document.getElementById('itemCount').textContent =
        data.count + ' results for "' + escHtml(query) + '"';
    })
    .catch(function() { showToast('Search failed', 'error'); });
}

// ═══════════════════════════════════════
//          UPLOAD (with speed meter)
// ═══════════════════════════════════════

function showUploadZone() {
  var zone = document.getElementById('uploadZone');
  zone.style.display = zone.style.display === 'none' ? 'block' : 'none';
}

function handleDragOver(e) {
  e.preventDefault();
  e.currentTarget.classList.add('drag-over');
}

function handleDragLeave(e) {
  e.currentTarget.classList.remove('drag-over');
}

function handleDrop(e) {
  e.preventDefault();
  e.currentTarget.classList.remove('drag-over');
  uploadFiles(e.dataTransfer.files);
}

function handleFileSelect(files) {
  uploadFiles(files);
}

function uploadFiles(files) {
  var progress = document.getElementById('uploadProgress');
  progress.style.display = 'block';
  var index = 0;
  var totalFiles = files.length;

  function uploadNext() {
    if (index >= totalFiles) {
      refresh();
      showToast(totalFiles + ' file(s) uploaded!', 'success');
      document.getElementById('speedBadge').style.display = 'none';
      return;
    }
    uploadSingleFile(files[index], function() {
      index++;
      uploadNext();
    });
  }
  uploadNext();
}

function uploadSingleFile(file, callback) {
  var progress = document.getElementById('uploadProgress');
  var itemId = 'p_' + Date.now() + '_' + Math.random().toString(36).substr(2,5);
  var div = document.createElement('div');
  div.className = 'progress-item';
  div.id = itemId;
  div.innerHTML = '<div class="progress-name">' +
    '<span>' + escHtml(file.name) + ' (' + formatBytes(file.size) + ')</span>' +
    '<span id="' + itemId + '_pct">0%</span></div>' +
    '<div class="progress-bar"><div class="progress-fill" id="' + itemId + '_bar"></div></div>';
  progress.appendChild(div);

  var xhr = new XMLHttpRequest();
  var formData = new FormData();
  formData.append('files', file);

  var startTime = Date.now();
  var lastLoaded = 0;
  var speedBadge = document.getElementById('speedBadge');

  xhr.upload.onprogress = function(e) {
    if (e.lengthComputable) {
      var pct = Math.round(e.loaded / e.total * 100);
      var bar = document.getElementById(itemId + '_bar');
      var pctEl = document.getElementById(itemId + '_pct');
      if (bar) bar.style.width = pct + '%';
      if (pctEl) pctEl.textContent = pct + '%';

      var elapsed = (Date.now() - startTime) / 1000;
      if (elapsed > 0.5) {
        var speed = (e.loaded - lastLoaded) / 0.5;
        lastLoaded = e.loaded;
        speedBadge.style.display = 'inline-block';
        speedBadge.textContent = formatBytes(speed) + '/s';
      }
    }
  };

  xhr.onload = function() {
    setTimeout(function() {
      if (div.parentNode) div.remove();
    }, 1500);
    if (callback) callback();
  };

  xhr.onerror = function() {
    showToast('Upload failed: ' + file.name, 'error');
    if (callback) callback();
  };

  xhr.open('POST', '/api/upload?path=' + encodeURIComponent(currentPath));
  xhr.send(formData);
}

// ═══════════════════════════════════════
//          FOLDER OPERATIONS
// ═══════════════════════════════════════

function showMkdirModal() {
  document.getElementById('folderNameInput').value = '';
  document.getElementById('mkdirModal').classList.add('show');
  setTimeout(function() {
    document.getElementById('folderNameInput').focus();
  }, 100);
}

function hideMkdirModal() {
  document.getElementById('mkdirModal').classList.remove('show');
}

function createFolder() {
  var name = document.getElementById('folderNameInput').value.trim();
  if (!name) return;

  fetch('/api/mkdir', {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: 'path=' + encodeURIComponent(currentPath) + '&name=' + encodeURIComponent(name)
  })
  .then(function(r) { return r.json(); })
  .then(function(data) {
    hideMkdirModal();
    if (data.success) { showToast('Folder created!', 'success'); refresh(); }
    else showToast(data.error, 'error');
  })
  .catch(function() { showToast('Failed to create folder', 'error'); });
}

// ═══════════════════════════════════════
//          RENAME
// ═══════════════════════════════════════

function showRenameModal(path, currentName) {
  renameTarget = path;
  document.getElementById('renameInput').value = currentName;
  document.getElementById('renameModal').classList.add('show');
  setTimeout(function() {
    var input = document.getElementById('renameInput');
    input.focus();
    var dot = currentName.lastIndexOf('.');
    input.setSelectionRange(0, dot > 0 ? dot : currentName.length);
  }, 100);
}

function hideRenameModal() {
  document.getElementById('renameModal').classList.remove('show');
  renameTarget = '';
}

function doRename() {
  var newName = document.getElementById('renameInput').value.trim();
  if (!newName || !renameTarget) return;

  fetch('/api/rename', {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: 'path=' + encodeURIComponent(renameTarget) + '&newName=' + encodeURIComponent(newName)
  })
  .then(function(r) { return r.json(); })
  .then(function(data) {
    hideRenameModal();
    if (data.success) { showToast('Renamed!', 'success'); refresh(); }
    else showToast(data.error, 'error');
  })
  .catch(function() { showToast('Rename failed', 'error'); });
}

// ═══════════════════════════════════════
//          DELETE SINGLE
// ═══════════════════════════════════════

function deleteItem(path, name) {
  if (!confirm('Delete "' + name + '"?')) return;

  fetch('/api/delete', {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: 'path=' + encodeURIComponent(path)
  })
  .then(function(r) { return r.json(); })
  .then(function(data) {
    if (data.success) { showToast('Deleted!', 'success'); refresh(); }
    else showToast(data.error, 'error');
  })
  .catch(function() { showToast('Delete failed', 'error'); });
}

// ═══════════════════════════════════════
//          TOAST
// ═══════════════════════════════════════

function showToast(message, type) {
  type = type || 'success';
  var container = document.getElementById('toastContainer');
  var toast = document.createElement('div');
  toast.className = 'toast ' + type;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(function() {
    if (toast.parentNode) toast.remove();
  }, 3500);
}

// ═══════════════════════════════════════
//          UTILS
// ═══════════════════════════════════════

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  var k = 1024;
  var sizes = ['B','KB','MB','GB'];
  var i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function escHtml(str) {
  return String(str)
    .replace(/&/g,'&amp;')
    .replace(/</g,'&lt;')
    .replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;')
    .replace(/'/g,'&#39;');
}

function escJs(str) {
  return String(str)
    .replace(/\\/g,'\\\\')
    .replace(/'/g,"\\'");
}"""
    }

    private fun buildLoginPage(): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>LocalShare - Login</title>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #0f172a; color: #f1f5f9;
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
}
.login-card {
  background: #1e293b; border: 1px solid #334155;
  border-radius: 16px; padding: 40px; width: 360px;
  box-shadow: 0 24px 48px rgba(0,0,0,0.5); text-align: center;
}
.login-icon { font-size: 48px; margin-bottom: 16px; }
.login-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
.login-sub { font-size: 14px; color: #94a3b8; margin-bottom: 28px; }
.login-input {
  width: 100%; padding: 12px 16px;
  background: #334155; border: 1px solid #475569;
  border-radius: 10px; color: #f1f5f9;
  font-size: 16px; outline: none; margin-bottom: 16px;
  text-align: center; letter-spacing: 4px;
}
.login-input:focus { border-color: #3b82f6; }
.login-btn {
  width: 100%; padding: 12px;
  background: #3b82f6; color: white;
  border: none; border-radius: 10px;
  font-size: 16px; font-weight: 600;
  cursor: pointer;
}
.login-btn:hover { background: #2563eb; }
.error-msg { color: #ef4444; font-size: 13px; margin-top: 12px; display: none; }
</style>
</head>
<body>
<div class="login-card">
  <div class="login-icon">&#128274;</div>
  <div class="login-title">LocalShare</div>
  <div class="login-sub">Enter password to access files</div>
  <input class="login-input" type="password" id="passwordInput"
         placeholder="&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;"
         onkeydown="if(event.key==='Enter') doLogin()">
  <button class="login-btn" onclick="doLogin()">Unlock</button>
  <div class="error-msg" id="errorMsg">Incorrect password</div>
</div>
<script>
function doLogin() {
  var pw = document.getElementById('passwordInput').value;
  var err = document.getElementById('errorMsg');
  err.style.display = 'none';
  fetch('/auth', {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: 'password=' + encodeURIComponent(pw)
  })
  .then(function(r) { return r.json(); })
  .then(function(data) {
    if (data.success) { window.location.href = '/'; }
    else {
      err.style.display = 'block';
      document.getElementById('passwordInput').value = '';
      document.getElementById('passwordInput').focus();
    }
  })
  .catch(function() { err.style.display = 'block'; });
}
document.getElementById('passwordInput').focus();
</script>
</body>
</html>"""
    }
}
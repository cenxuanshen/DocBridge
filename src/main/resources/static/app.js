// ============================================================
// FormatBridge - 前端主逻辑
// ============================================================

let selectedFiles = [];
let isConverting = false;
let progressInterval = null;

// ===== DOM 元素 =====
const uploadArea = document.getElementById('uploadArea');
const fileInput = document.getElementById('fileInput');
const fileInfo = document.getElementById('fileInfo');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const fileTypeBadge = document.getElementById('fileTypeBadge');
const removeFileBtn = document.getElementById('removeFile');
const convertBtn = document.getElementById('convertBtn');
const fromFormat = document.getElementById('fromFormat');
const toFormat = document.getElementById('toFormat');
const resultArea = document.getElementById('resultArea');
const resultStatus = document.getElementById('resultStatus');
const resultMessage = document.getElementById('resultMessage');
const downloadBtn = document.getElementById('downloadBtn');
const progressContainer = document.getElementById('progressContainer');
const progressBar = document.getElementById('progressBar');
const progressText = document.getElementById('progressText');
const themeToggle = document.getElementById('themeToggle');

// ===== 常量 =====
const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
const VALID_EXTENSIONS = ['.pdf', '.docx', '.xlsx', '.html', '.txt', '.pptx'];
const FILE_ICONS = {
    'pdf': '📄',
    'docx': '📝',
    'xlsx': '📊',
    'html': '🌐',
    'txt': '📃',
    'pptx': '📽️'
};
const SUPPORTED_PAIRS = [
    'docx→pdf', 'pdf→docx',
    'xlsx→pdf',
    'html→pdf', 'pdf→html',
    'pptx→pdf'
];

// ===== 进度条 =====
function startProgress() {
    progressContainer.style.display = 'block';
    progressBar.style.width = '0%';
    progressText.textContent = '0%';

    let progress = 0;
    clearInterval(progressInterval);
    progressInterval = setInterval(() => {
        if (progress < 90) {
            progress += Math.random() * 8 + 2;
            if (progress > 90) progress = 90;
            progressBar.style.width = progress + '%';
            progressText.textContent = Math.floor(progress) + '%';
        }
    }, 200);
}

function stopProgress() {
    clearInterval(progressInterval);
    progressBar.style.width = '100%';
    progressText.textContent = '100%';
    setTimeout(() => {
        progressContainer.style.display = 'none';
    }, 500);
}

function resetProgress() {
    clearInterval(progressInterval);
    progressContainer.style.display = 'none';
    progressBar.style.width = '0%';
    progressText.textContent = '0%';
}

// ===== 文件上传 =====
uploadArea.addEventListener('click', () => fileInput.click());

uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.classList.add('dragover');
});

uploadArea.addEventListener('dragleave', () => {
    uploadArea.classList.remove('dragover');
});

uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
    if (e.dataTransfer.files.length) {
        handleFiles(e.dataTransfer.files);
    }
});

fileInput.addEventListener('change', () => {
    if (fileInput.files.length) {
        handleFiles(fileInput.files);
    }
});

// ===== 处理文件 =====
function handleFiles(files) {
    // 清空之前的选择
    selectedFiles = [];
    fileInfo.style.display = 'none';
    uploadArea.style.display = 'none';
    resetProgress();
    resultArea.style.display = 'none';

    let validCount = 0;

    for (let file of files) {
        // 检查文件大小
        if (file.size > MAX_FILE_SIZE) {
            const sizeMB = (file.size / 1024 / 1024).toFixed(1);
            alert('⚠️ 文件 ' + file.name + ' 过大（' + sizeMB + 'MB），最大支持 50MB');
            continue;
        }

        // 检查文件格式
        const fileNameLower = file.name.toLowerCase();
        const isValid = VALID_EXTENSIONS.some(ext => fileNameLower.endsWith(ext));
        if (!isValid) {
            alert('⚠️ 文件 ' + file.name + ' 格式不支持');
            continue;
        }

        selectedFiles.push(file);
        validCount++;
    }

    if (selectedFiles.length === 0) {
        uploadArea.style.display = 'block';
        fileInfo.style.display = 'none';
        return;
    }

    // 显示文件信息
    if (selectedFiles.length === 1) {
        const file = selectedFiles[0];
        fileName.textContent = file.name;
        fileSize.textContent = (file.size / 1024).toFixed(1) + ' KB';
        const ext = file.name.split('.').pop().toLowerCase();
        fileTypeBadge.textContent = FILE_ICONS[ext] || '📄';
    } else {
        const totalSize = selectedFiles.reduce((sum, f) => sum + f.size, 0);
        fileName.textContent = '已选择 ' + selectedFiles.length + ' 个文件';
        fileSize.textContent = (totalSize / 1024).toFixed(1) + ' KB';
        fileTypeBadge.textContent = '📚';
    }

    fileInfo.style.display = 'flex';
    uploadArea.style.display = 'none';
}

// ===== 移除文件 =====
removeFileBtn.addEventListener('click', () => {
    selectedFiles = [];
    fileInfo.style.display = 'none';
    uploadArea.style.display = 'block';
    resultArea.style.display = 'none';
    fileInput.value = '';
    resetProgress();
});

// ===== 格式联动 =====
fromFormat.addEventListener('change', () => {
    if (fromFormat.value === toFormat.value) {
        const options = [...toFormat.options];
        const next = options.find(opt => opt.value !== fromFormat.value);
        if (next) toFormat.value = next.value;
    }
});

toFormat.addEventListener('change', () => {
    if (toFormat.value === fromFormat.value) {
        const options = [...fromFormat.options];
        const next = options.find(opt => opt.value !== toFormat.value);
        if (next) fromFormat.value = next.value;
    }
});

// ===== 转换按钮 =====
convertBtn.addEventListener('click', async () => {
    if (selectedFiles.length === 0) {
        alert('⚠️ 请先上传文件');
        return;
    }

    const from = fromFormat.value;
    const to = toFormat.value;

    if (from === to) {
        alert('⚠️ 请选择不同的格式进行转换');
        return;
    }

    // 检查是否支持
    const pairKey = from + '→' + to;
    if (!SUPPORTED_PAIRS.includes(pairKey)) {
        alert('⚠️ 暂不支持 ' + from.toUpperCase() + ' → ' + to.toUpperCase() + ' 转换');
        return;
    }

    // 只处理第一个文件（批量转换后续优化）
    const file = selectedFiles[0];

    // 显示进度
    startProgress();
    resultArea.style.display = 'block';
    resultStatus.className = 'result-status loading';
    resultStatus.textContent = '⏳ 正在转换...';
    resultMessage.textContent = '请稍候，文件正在处理中...';
    downloadBtn.style.display = 'none';
    convertBtn.disabled = true;
    isConverting = true;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('fromFormat', from);
    formData.append('toFormat', to);

    try {
        const response = await fetch('/convert', {
            method: 'POST',
            body: formData
        });

        stopProgress();

        if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            downloadBtn.href = url;

            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = 'converted.' + to;
            if (contentDisposition) {
                const match = contentDisposition.match(/filename="(.+)"/);
                if (match) filename = match[1];
            }
            downloadBtn.download = filename;
            downloadBtn.style.display = 'inline-block';
            resultStatus.className = 'result-status success';
            resultStatus.textContent = '✅ 转换成功！';
            resultMessage.textContent = '点击下方按钮下载转换后的文件';
        } else {
            const error = await response.text();
            resultStatus.className = 'result-status error';
            resultStatus.textContent = '❌ 转换失败';
            resultMessage.textContent = error || '未知错误，请重试';
        }
    } catch (err) {
        stopProgress();
        resultStatus.className = 'result-status error';
        resultStatus.textContent = '❌ 请求失败';
        resultMessage.textContent = err.message || '网络错误，请检查连接';
    } finally {
        convertBtn.disabled = false;
        isConverting = false;
    }
});

// ============================================================
// 深色模式
// ============================================================
if (localStorage.getItem('theme') === 'dark') {
    document.body.classList.add('dark-mode');
    if (themeToggle) themeToggle.textContent = '☀️';
}

if (themeToggle) {
    themeToggle.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        if (document.body.classList.contains('dark-mode')) {
            localStorage.setItem('theme', 'dark');
            themeToggle.textContent = '☀️';
        } else {
            localStorage.setItem('theme', 'light');
            themeToggle.textContent = '🌙';
        }
    });
}

console.log('🌉 FormatBridge 已加载');
console.log('📂 支持格式: ' + VALID_EXTENSIONS.join(', '));
console.log('📏 最大文件: 50MB');
console.log('🔄 支持转换: ' + SUPPORTED_PAIRS.join(', '));
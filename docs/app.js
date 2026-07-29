// Elements
const btnNormal = document.getElementById('btnNormal');
const btnSpike = document.getElementById('btnSpike');
const btnAttack = document.getElementById('btnAttack');

const valHR = document.getElementById('valHR');
const valAudio = document.getElementById('valAudio');
const valTamper = document.getElementById('valTamper');
const iconTamper = document.getElementById('iconTamper');
const systemStatus = document.getElementById('systemStatus');
const ekgPolyline = document.getElementById('ekgPolyline');
const eventLog = document.getElementById('eventLog');
const alarmSound = document.getElementById('alarmSound');
const audioBars = document.querySelectorAll('.bar');

// State
let currentState = 'normal';
let hrInterval;
let audioInterval;

// Helpers
function logEvent(message, isAlert = false) {
    const time = new Date().toLocaleTimeString('en-US', { hour12: false });
    const li = document.createElement('li');
    li.innerHTML = `<span class="time">${time}</span> <span class="log-msg ${isAlert ? 'alert-text' : ''}">${message}</span>`;
    eventLog.prepend(li);
}

function updateEKG(baseHR, variance) {
    const points = [];
    let x = 0;
    while (x <= 500) {
        points.push(`${x},50`);
        x += Math.floor(Math.random() * 20) + 20;
        
        if (x < 500) {
            points.push(`${x},50`);
            points.push(`${x+5},20`);
            points.push(`${x+15},80`);
            points.push(`${x+20},50`);
            x += 20;
        }
    }
    ekgPolyline.setAttribute('points', points.join(' '));
    
    const hr = Math.floor(baseHR + (Math.random() * variance * 2 - variance));
    valHR.innerText = `${hr} BPM`;
    
    if (hr > 120) {
        valHR.classList.add('danger');
        valHR.classList.remove('warning');
        ekgPolyline.parentElement.classList.add('danger');
    } else if (hr > 100) {
        valHR.classList.add('warning');
        valHR.classList.remove('danger');
        ekgPolyline.parentElement.classList.remove('danger');
    } else {
        valHR.classList.remove('danger', 'warning');
        ekgPolyline.parentElement.classList.remove('danger');
    }
}

function updateAudio(baseDb, variance) {
    const db = Math.floor(baseDb + (Math.random() * variance * 2 - variance));
    valAudio.innerText = `${db} dB`;
    
    audioBars.forEach(bar => {
        const height = Math.max(10, Math.random() * db);
        bar.style.height = `${height}%`;
        
        if (db > 80) {
            bar.classList.add('danger');
            bar.classList.remove('warning');
        } else if (db > 60) {
            bar.classList.add('warning');
            bar.classList.remove('danger');
        } else {
            bar.classList.remove('danger', 'warning');
        }
    });
    
    if (db > 80) {
        valAudio.classList.add('danger');
        valAudio.classList.remove('warning');
    } else if (db > 60) {
        valAudio.classList.add('warning');
        valAudio.classList.remove('danger');
    } else {
        valAudio.classList.remove('danger', 'warning');
    }
}

// Modes
function setNormalMode() {
    currentState = 'normal';
    clearInterval(hrInterval);
    clearInterval(audioInterval);
    
    btnNormal.className = 'btn btn-primary active';
    btnSpike.className = 'btn btn-secondary';
    btnAttack.className = 'btn btn-secondary';
    
    valTamper.innerText = 'Attached';
    valTamper.className = 'value success';
    iconTamper.innerHTML = '<i class="fa-solid fa-lock"></i>';
    iconTamper.className = 'tamper-icon';
    
    systemStatus.innerHTML = '<i class="fa-solid fa-shield-halved"></i> SYSTEM ACTIVE';
    systemStatus.className = 'status-badge';
    alarmSound.pause();
    
    hrInterval = setInterval(() => updateEKG(75, 5), 1000);
    audioInterval = setInterval(() => updateAudio(45, 10), 200);
    
    logEvent('System returned to normal monitoring.');
}

function setSpikeMode() {
    if (currentState === 'spike') return;
    currentState = 'spike';
    clearInterval(hrInterval);
    clearInterval(audioInterval);
    
    btnNormal.className = 'btn btn-secondary';
    btnSpike.className = 'btn btn-primary active';
    btnAttack.className = 'btn btn-secondary';
    
    logEvent('Child playing/running detected. HR elevated.');
    logEvent('Fusion Engine: Audio normal. Ignoring HR spike (False Positive avoided).');
    
    hrInterval = setInterval(() => updateEKG(135, 15), 500);
    audioInterval = setInterval(() => updateAudio(55, 15), 200);
}

function setAttackMode() {
    if (currentState === 'attack') return;
    currentState = 'attack';
    clearInterval(hrInterval);
    clearInterval(audioInterval);
    
    btnNormal.className = 'btn btn-secondary';
    btnSpike.className = 'btn btn-secondary';
    btnAttack.className = 'btn danger active';
    
    valTamper.innerText = 'REMOVED!';
    valTamper.className = 'value danger';
    iconTamper.innerHTML = '<i class="fa-solid fa-unlock-keyhole"></i>';
    iconTamper.className = 'tamper-icon danger';
    
    systemStatus.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> TIER 1 ALERT TRIGGERED';
    systemStatus.className = 'status-badge alert';
    
    let playPromise = alarmSound.play();
    if (playPromise !== undefined) {
        playPromise.catch(error => {
            console.log("Audio autoplay prevented by browser. User interaction needed.");
        });
    }
    
    logEvent('Device Tampered! Proximity sensor triggered.', true);
    logEvent('Fusion Engine: High HR + High Audio + Tamper detected.', true);
    logEvent('Escalation: Tier 1 webhook sent to Safe Adult via Telegram.', true);
    
    hrInterval = setInterval(() => updateEKG(145, 20), 400);
    audioInterval = setInterval(() => updateAudio(95, 10), 100);
    
    setTimeout(() => {
        if (currentState === 'attack') {
            logEvent('Escalation: No ACK from Tier 1. Escalating to Tier 3 (PO Intervention).', true);
            systemStatus.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> TIER 3 ESCALATION';
        }
    }, 5000);
}

// Listeners
btnNormal.addEventListener('click', setNormalMode);
btnSpike.addEventListener('click', setSpikeMode);
btnAttack.addEventListener('click', setAttackMode);

// Initialize
setNormalMode();

// Elements
const btnNormal = document.getElementById('btnNormal');
const btnSpike = document.getElementById('btnSpike');
const btnAttack = document.getElementById('btnAttack');

const valHR = document.getElementById('valHR');
const valAudio = document.getElementById('valAudio');
const valAudioLabel = document.getElementById('valAudioLabel');
const valTamper = document.getElementById('valTamper');
const iconTamper = document.getElementById('iconTamper');
const systemStatus = document.getElementById('systemStatus');
const ekgPolyline = document.getElementById('ekgPolyline');
const eventLog = document.getElementById('eventLog');
const audioBars = document.querySelectorAll('.bar');

// POINT 4: No alarm sound (Telegram/WhatsApp removed, MSF-approved channel only)
// POINT 2: No automatic siren on tamper

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

// POINT 5: Audio event classification (mirrors FusionEngine.kt logic)
function classifyAudioEvent(currentDb, previousDb) {
    const delta = previousDb !== null ? currentDb - previousDb : 0;
    if (currentDb >= 100 && delta >= 15) return 'IMPACT';
    if (currentDb >= 85 && Math.abs(delta) >= 5) return 'SHOUTING';
    if (currentDb >= 70 && currentDb <= 85 && Math.abs(delta) >= 2 && Math.abs(delta) <= 8) return 'CRYING';
    return 'AMBIENT';
}

let previousDb = null;

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
        ekgPolyline.parentElement.classList.add('danger');
    } else {
        valHR.classList.remove('danger');
        ekgPolyline.parentElement.classList.remove('danger');
    }
}

function updateAudio(baseDb, variance) {
    const db = Math.floor(baseDb + (Math.random() * variance * 2 - variance));
    const label = classifyAudioEvent(db, previousDb);
    previousDb = db;

    valAudio.innerText = `${db} dB`;
    valAudioLabel.innerText = label;

    const distress = ['SHOUTING', 'CRYING', 'IMPACT'];
    const isDistress = distress.includes(label);

    audioBars.forEach(bar => {
        const height = Math.max(10, Math.random() * db);
        bar.style.height = `${height}%`;
        bar.classList.toggle('danger', isDistress);
    });

    valAudio.classList.toggle('danger', isDistress);
    valAudioLabel.className = 'audio-label ' + (isDistress ? 'label-danger' : 'label-safe');
}

function setNormalMode() {
    currentState = 'normal';
    clearInterval(hrInterval);
    clearInterval(audioInterval);
    previousDb = null;

    btnNormal.className = 'btn btn-primary active';
    btnSpike.className = 'btn btn-secondary';
    btnAttack.className = 'btn btn-secondary';

    valTamper.innerText = 'Attached';
    valTamper.className = 'value success';
    iconTamper.innerHTML = '<i class="fa-solid fa-lock"></i>';
    iconTamper.className = 'tamper-icon';

    systemStatus.innerHTML = '<i class="fa-solid fa-shield-halved"></i> SYSTEM ACTIVE';
    systemStatus.className = 'status-badge';

    hrInterval = setInterval(() => updateEKG(75, 5), 1000);
    audioInterval = setInterval(() => updateAudio(45, 10), 200);

    logEvent('System returned to normal monitoring. Audio classifier active.');
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
    logEvent('Audio Classifier: AMBIENT. Fusion Engine suppresses alert (False Positive avoided).');

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

    // POINT 2: Silent tamper alert — no siren
    logEvent('Device tampered! Silent alert sent. Tamper event recorded in encrypted log.', true);
    // POINT 5: Audio classifier active during attack
    logEvent('Audio Classifier: SHOUTING detected. Fusion Engine: HR + SHOUTING = confirmed.', true);
    // POINT 4: No Telegram/WhatsApp mention — MSF-approved channel
    logEvent('Tier 1: Alert routed via MSF-approved secure channel. Incident ID #GP-' + Math.floor(Math.random()*9999), true);

    hrInterval = setInterval(() => updateEKG(145, 20), 400);
    audioInterval = setInterval(() => updateAudio(95, 10), 100);

    // POINT 3: Configurable escalation timeout (demo uses 5s)
    setTimeout(() => {
        if (currentState === 'attack') {
            logEvent('No ACK received. Escalating to Tier 3 (Protection Officer). Timeout: per MSF protocol.', true);
            systemStatus.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> TIER 3 ESCALATION';
        }
    }, 5000);
}

btnNormal.addEventListener('click', setNormalMode);
btnSpike.addEventListener('click', setSpikeMode);
btnAttack.addEventListener('click', setAttackMode);

setNormalMode();

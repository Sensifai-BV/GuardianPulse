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
    li.innerHTML = `<span class="time">${time}</span> <span class="${isAlert ? 'alert-text' : ''}">${message}</span>`;
    eventLog.prepend(li);
}

function updateEKG(baseHR, variance) {
    // Generate a random EKG-like wave based on heart rate
    // Higher HR = more squished wave (we simulate this by changing the points)
    const points = [];
    let x = 0;
    while (x <= 500) {
        points.push(`${x},50`);
        x += Math.floor(Math.random() * 20) + 20; // flat line
        
        if (x < 500) {
            // The spike
            points.push(`${x},50`);
            points.push(`${x+5},20`);
            points.push(`${x+15},80`);
            points.push(`${x+20},50`);
            x += 20;
        }
    }
    ekgPolyline.setAttribute('points', points.join(' '));
    
    // Update text
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
    valAudio.innerText = `${db} dB`;
    
    audioBars.forEach(bar => {
        // Random height for each bar based on db level
        const height = Math.max(10, Math.random() * db);
        bar.style.height = `${height}%`;
        
        if (db > 80) {
            bar.classList.add('danger');
        } else {
            bar.classList.remove('danger');
        }
    });
    
    if (db > 80) {
        valAudio.classList.add('danger');
    } else {
        valAudio.classList.remove('danger');
    }
}

// Modes
function setNormalMode() {
    currentState = 'normal';
    clearInterval(hrInterval);
    clearInterval(audioInterval);
    
    btnNormal.classList.add('active');
    btnSpike.classList.remove('active');
    btnAttack.classList.remove('active');
    
    valTamper.innerText = 'Attached';
    valTamper.classList.remove('danger');
    iconTamper.innerHTML = '<i class="fa-solid fa-lock"></i>';
    iconTamper.classList.remove('danger');
    
    systemStatus.innerHTML = '<i class="fa-solid fa-shield-halved"></i> SYSTEM ACTIVE';
    systemStatus.classList.remove('alert');
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
    
    btnNormal.classList.remove('active');
    btnSpike.classList.add('active');
    btnAttack.classList.remove('active');
    
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
    
    btnNormal.classList.remove('active');
    btnSpike.classList.remove('active');
    btnAttack.classList.add('active');
    
    valTamper.innerText = 'REMOVED!';
    valTamper.classList.add('danger');
    iconTamper.innerHTML = '<i class="fa-solid fa-unlock-keyhole"></i>';
    iconTamper.classList.add('danger');
    
    systemStatus.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> TIER 1 ALERT TRIGGERED';
    systemStatus.classList.add('alert');
    
    // Play sound but catch autoplay restrictions
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
    
    // Simulate Tier 3 escalation after 5 seconds if not normalized
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

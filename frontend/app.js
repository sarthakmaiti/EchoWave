let stompClient = null;
let currentChannelId = null;
let peerConnections = {};
let listeners = new Set();

let jwt = localStorage.getItem("jwt");
let username = localStorage.getItem("username");

let localStream = null;
let isSpeaker = false;

// unlock audio on first interaction
let audioUnlocked = false;
document.addEventListener(
  "click",
  () => {
    audioUnlocked = true;
    console.log("🔓 Audio unlocked by user gesture");
  },
  { once: true },
);

updateUI();

/* ================= UI ================= */

function log(msg) {
  const logEl = document.getElementById("log");
  logEl.textContent += msg + "\n";
  logEl.scrollTop = logEl.scrollHeight;
}

function updateUI() {
  const loggedIn = jwt !== null;
  document.getElementById("auth").style.display = loggedIn ? "none" : "block";
  document.getElementById("app").style.display = loggedIn ? "block" : "none";
  if (loggedIn) document.getElementById("currentUser").innerText = username;
}

function login() {
  const u = usernameInput();
  const p = passwordInput();

  fetch("https://echowavevoices.com/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: u, password: p }),
  })
    .then((res) => {
      if (!res.ok) throw new Error("Login failed");
      return res.json();
    })
    .then((data) => {
      jwt = data.token;
      username = u;
      localStorage.setItem("jwt", jwt);
      localStorage.setItem("username", username);
      log("✅ Logged in");
      updateUI();
    })
    .catch((err) => log("❌ Login failed: " + err.message));
}

function register() {
  const u = usernameInput();
  const p = passwordInput();

  fetch("https://echowavevoices.com/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: u, password: p }),
  })
    .then((res) => {
      if (!res.ok) throw new Error("Register failed");
      log("✅ Registered successfully. You can now log in.");
    })
    .catch((err) => log("❌ Register failed: " + err.message));
}

function usernameInput() {
  return document.getElementById("username").value.trim();
}

function passwordInput() {
  return document.getElementById("password").value;
}

/* ================= MIC ================= */

async function startMic() {
  if (localStream) {
    log("🎙️ Microphone already active");
    return;
  }

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    log("❌ Microphone access not available (requires HTTPS)");
    console.error("navigator.mediaDevices is undefined – must use HTTPS");
    alert(
      "Microphone access requires HTTPS. Make sure you're using https://echowavevoices.com",
    );
    return;
  }

  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: false,
        noiseSuppression: true,
        autoGainControl: true,
        channelCount: 1,
        sampleSize: 16,
      },
      video: false,
    });

    log("🎙️ Microphone started successfully");
    console.log("Local stream tracks:", localStream.getTracks().length);
  } catch (err) {
    console.error("getUserMedia error:", err);
    if (
      err.name === "NotAllowedError" ||
      err.name === "PermissionDeniedError"
    ) {
      log(
        "❌ Microphone permission denied. Please allow access in browser settings.",
      );
      alert("Microphone permission was denied. Allow it and try again.");
    } else if (err.name === "NotFoundError") {
      log("❌ No microphone detected on this device.");
    } else {
      log("❌ Failed to start microphone: " + err.message);
    }
  }
}

function stopMic() {
  if (!localStream) return;
  localStream.getTracks().forEach((t) => t.stop());
  localStream = null;
  log("🎙️ Microphone stopped");
}

/* ================= WEBRTC ================= */

function createPeerConnection(remoteUser) {
  const pc = new RTCPeerConnection({
    iceServers: [
      { urls: "stun:stun.l.google.com:19302" },
      { urls: "stun:stun1.l.google.com:19302" },
      { urls: "stun:stun2.l.google.com:19302" },
      { urls: "stun:stun3.l.google.com:19302" },
      { urls: "stun:stun4.l.google.com:19302" },
      {
        urls: "turn:18.61.161.160:3478",
        username: "echowave",
        credential: "strongpassword123",
      },
    ],
    bundlePolicy: "max-bundle",
    rtcpMuxPolicy: "require",
  });

  if (isSpeaker && localStream) {
    localStream.getTracks().forEach((track) => {
      pc.addTrack(track, localStream);
      console.log("🎤 Audio track added for", remoteUser);
    });
    const sender = pc.getSenders().find((s) => s.track?.kind === "audio");
    if (sender) {
      const params = sender.getParameters() || {};
      if (!params.encodings) params.encodings = [{}];
      params.encodings[0].maxBitrate = 128000;
      sender
        .setParameters(params)
        .catch((e) => console.warn("Bitrate set failed:", e));
    }
  }

  pc.ontrack = (event) => {
    console.log("🔊 Received audio from", remoteUser);
    const audio = document.createElement("audio");
    audio.srcObject = event.streams[0];
    audio.autoplay = true;
    audio.muted = false;
    audio.controls = true;
    audio.id = `audio-${remoteUser}`;
    document.body.appendChild(audio);

    audio.play().catch((e) => console.warn("Audio playback failed:", e));

    if (event.receiver?.jitterBufferTarget !== undefined) {
      event.receiver.jitterBufferTarget = 100; // ms
      console.log("Jitter buffer set to 100ms");
    }
  };

  pc.onicecandidate = (event) => {
    if (event.candidate && stompClient?.connected) {
      stompClient.send(
        "/app/webrtc/ice",
        {},
        JSON.stringify({
          from: username,
          to: remoteUser,
          candidate: event.candidate,
        }),
      );
    }
  };

  pc.oniceconnectionstatechange = () =>
    console.log(`ICE ${remoteUser}: ${pc.iceConnectionState}`);
  pc.onconnectionstatechange = () =>
    console.log(`Connection ${remoteUser}: ${pc.connectionState}`);

  // Stats logging
  setInterval(async () => {
    if (pc?.remoteDescription) {
      const stats = await pc.getStats();
      stats.forEach((report) => {
        if (report.type === "inbound-rtp" && report.kind === "audio") {
          log(
            `📊 ${remoteUser} audio: lost=${report.packetsLost || 0}, jitter=${report.jitter || 0}`,
          );
        }
      });
    }
  }, 5000);

  peerConnections[remoteUser] = pc;
  return pc;
}

function closeAllPeerConnections() {
  Object.values(peerConnections).forEach((pc) => pc.close());
  peerConnections = {};
  document.querySelectorAll("audio").forEach((a) => a.remove());
  log("❌ All WebRTC connections closed");
}

/* ================= OFFER HANDLING ================= */

async function createOfferFor(remoteUser) {
  if (!stompClient?.connected) return;
  console.log("📤 Creating offer for", remoteUser);

  const pc = createPeerConnection(remoteUser);
  try {
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    stompClient.send(
      "/app/webrtc/sdp",
      {},
      JSON.stringify({
        type: "offer",
        from: username,
        to: remoteUser,
        sdp: offer.sdp,
      }),
    );
  } catch (err) {
    console.error("Offer failed:", err);
  }
}

/* ================= WEBSOCKET ================= */

function connect() {
  if (!jwt) {
    alert("Please login first");
    return;
  }

  currentChannelId = document.getElementById("channelId").value.trim();
  if (!currentChannelId) {
    alert("Please enter a channel ID");
    return;
  }

  listeners.clear();

  const socket = new SockJS("https://echowavevoices.com/ws");
  stompClient = Stomp.over(socket);

  stompClient.connect(
    { Authorization: "Bearer " + jwt },
    () => {
      log(`🔗 Connected to channel ${currentChannelId}`);

      stompClient.subscribe(`/topic/channel/${currentChannelId}`, (msg) => {
        const event = JSON.parse(msg.body);
        log("EVENT: " + JSON.stringify(event, null, 2));

        if (event.type === "JOIN" || event.type === "LEAVE") {
          listeners.clear();
          (event.users || []).forEach((u) => listeners.add(u));
          log(`👥 Users: ${Array.from(listeners).join(", ") || "none"}`);

          if (event.type === "JOIN" && isSpeaker && event.user !== username) {
            createOfferFor(event.user);
          }

          if (event.type === "LEAVE" && event.user) {
            if (peerConnections[event.user]) {
              peerConnections[event.user].close();
              delete peerConnections[event.user];
              document.getElementById(`audio-${event.user}`)?.remove();
            }
          }
        }

        if (event.type === "SPEAKER_GRANTED") {
          if (event.user === username) {
            onSpeakerGranted();
          } else {
            log(`🎤 New speaker: ${event.user}`);
          }
        }

        if (event.type === "SPEAKER_RELEASED") {
          if (event.user === username) {
            isSpeaker = false;
            stopMic();
          }
          closeAllPeerConnections();
          log(`🔇 Speaker released: ${event.user}`);
        }
      });

      stompClient.subscribe("/user/queue/webrtc", async (msg) => {
        const data = JSON.parse(msg.body);

        if (data.type === "offer") {
          console.log("📥 Offer from", data.from);
          const pc = createPeerConnection(data.from);
          await pc.setRemoteDescription({ type: "offer", sdp: data.sdp });
          const answer = await pc.createAnswer();
          await pc.setLocalDescription(answer);
          stompClient.send(
            "/app/webrtc/sdp",
            {},
            JSON.stringify({
              type: "answer",
              from: username,
              to: data.from,
              sdp: answer.sdp,
            }),
          );
        }

        if (data.type === "answer") {
          const pc = peerConnections[data.from];
          if (pc)
            await pc.setRemoteDescription({ type: "answer", sdp: data.sdp });
        }

        if (data.candidate) {
          const pc = peerConnections[data.from];
          if (pc) await pc.addIceCandidate(data.candidate);
        }
      });

      stompClient.subscribe("/user/queue/speaker-denied", (msg) => {
        log("❌ Speaker request denied");
      });
    },
    (err) => {
      log("❌ WebSocket error: " + err);
      console.error("STOMP connection error:", err);
    },
  );
}

async function onSpeakerGranted() {
  isSpeaker = true;
  log("🎤 You are now the speaker");

  try {
    await startMic();
    for (const user of listeners) {
      if (user !== username) await createOfferFor(user);
    }
  } catch (err) {
    log("❌ Failed to start speaking: " + err.message);
    isSpeaker = false;
  }
}

function onSpeakerReleased() {
  isSpeaker = false;
  stopMic();
  closeAllPeerConnections();
  log("🔇 Speaker role released");
}

function logout() {
  console.log("👋 Logging out");

  if (stompClient) {
    stompClient.disconnect(() => console.log("🔌 WebSocket disconnected"));
    stompClient = null;
  }

  onSpeakerReleased();

  localStorage.removeItem("jwt");
  localStorage.removeItem("username");
  jwt = null;
  username = null;
  currentChannelId = null;

  log("👋 Logged out");
  updateUI();
}

/* ================= HELPERS ================= */

function requestSpeaker() {
  if (!stompClient?.connected) {
    log("❌ Not connected to server");
    return;
  }
  stompClient.send(
    `/app/channel/${currentChannelId}/speaker/request`,
    {},
    "{}",
  );
  log("📤 Requested speaker role...");
}

function releaseSpeaker() {
  if (!stompClient?.connected) return;
  stompClient.send(
    `/app/channel/${currentChannelId}/speaker/release`,
    {},
    "{}",
  );
  log("📤 Released speaker role");
}

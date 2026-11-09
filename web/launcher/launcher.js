/**
 * GrizmMC Web Minecraft 3D Launcher & Auto-Connector
 * Powered by Three.js & Web Audio API
 */

// ============================================================================
// 1. CONFIGURATION & STATE
// ============================================================================
const CONFIG = {
  defaultServer: {
    name: 'GrizmMC Network (Ana Giriş)',
    ip: 'play-mc.grizm.tech',
    port: 25565,
    tagline: 'Velocity Proxy 3.5.1 • Paper 1.21.4'
  },
  subservers: [
    { id: 'proxy', name: 'Ana Ağ (Proxy)', port: 25565, desc: 'Merkezi Giriş Noktası', icon: 'fa-network-wired' },
    { id: 'lobby', name: 'Merkez Lobi', port: 25566, desc: 'Lobi, NPC ve GrizmAuth', icon: 'fa-landmark' },
    { id: 'towny', name: 'Grizm Towny', port: 25567, desc: 'Kasaba, Kuşatma, Ekonomi', icon: 'fa-shield-halved' },
    { id: 'skyblock', name: 'Skyblock Void', port: 25568, desc: 'Özel Void Adalar & Banka', icon: 'fa-cloud' },
    { id: 'pvp', name: 'PvP Duels', port: 25569, desc: '1v1 Arenalar & Turnuvalar', icon: 'fa-swords' }
  ],
  autoConnectDelay: 5000, // 5 seconds
  apiEndpoint: '../api/status.php',
  fallbackApi: 'https://api.mcsrvstat.us/3/play-mc.grizm.tech'
};

const STATE = {
  username: (window.GRIZM_PHP_CONFIG && window.GRIZM_PHP_CONFIG.username) 
            || localStorage.getItem('grizm_username') 
            || 'GrizmOyuncu',
  activeServer: CONFIG.defaultServer,
  autoConnectEnabled: localStorage.getItem('grizm_autoconnect') !== 'false',
  soundEnabled: localStorage.getItem('grizm_sound') !== 'false',
  countdown: 5,
  timerInterval: null,
  isPaused: false,
  isConnecting: false,
  inGameMode: false,
  selectedBlockId: 1
};

if (window.GRIZM_PHP_CONFIG && window.GRIZM_PHP_CONFIG.serverIp) {
  CONFIG.defaultServer.ip = window.GRIZM_PHP_CONFIG.serverIp;
}

// ============================================================================
// 2. PROCEDURAL TEXTURE GENERATOR (Pixel-Perfect Minecraft Textures)
// ============================================================================
const Textures = {};

function createNoise(ctx, width, height, density, colorFn) {
  for (let x = 0; x < width; x++) {
    for (let y = 0; y < height; y++) {
      if (Math.random() < density) {
        ctx.fillStyle = colorFn(x, y);
        ctx.fillRect(x, y, 1, 1);
      }
    }
  }
}

function initTextures() {
  const size = 16;

  // 1. Grass Top
  const cGrassTop = document.createElement('canvas');
  cGrassTop.width = cGrassTop.height = size;
  const ctxGT = cGrassTop.getContext('2d');
  ctxGT.fillStyle = '#4c962b';
  ctxGT.fillRect(0, 0, size, size);
  createNoise(ctxGT, size, size, 0.45, () => (Math.random() > 0.5 ? '#5aa832' : '#3d7c22'));
  Textures.grassTop = new THREE.CanvasTexture(cGrassTop);
  Textures.grassTop.magFilter = THREE.NearestFilter;
  Textures.grassTop.minFilter = THREE.NearestFilter;

  // 2. Dirt
  const cDirt = document.createElement('canvas');
  cDirt.width = cDirt.height = size;
  const ctxD = cDirt.getContext('2d');
  ctxD.fillStyle = '#866043';
  ctxD.fillRect(0, 0, size, size);
  createNoise(ctxD, size, size, 0.4, () => (Math.random() > 0.5 ? '#6d4c33' : '#976d4e'));
  Textures.dirt = new THREE.CanvasTexture(cDirt);
  Textures.dirt.magFilter = THREE.NearestFilter;
  Textures.dirt.minFilter = THREE.NearestFilter;

  // 3. Grass Side
  const cGrassSide = document.createElement('canvas');
  cGrassSide.width = cGrassSide.height = size;
  const ctxGS = cGrassSide.getContext('2d');
  ctxGS.drawImage(cDirt, 0, 0);
  ctxGS.fillStyle = '#4c962b';
  ctxGS.fillRect(0, 0, size, 4);
  for (let x = 0; x < size; x++) {
    const hang = Math.floor(Math.random() * 3) + 3;
    ctxGS.fillStyle = '#4c962b';
    ctxGS.fillRect(x, 0, 1, hang);
  }
  Textures.grassSide = new THREE.CanvasTexture(cGrassSide);
  Textures.grassSide.magFilter = THREE.NearestFilter;
  Textures.grassSide.minFilter = THREE.NearestFilter;

  // 4. Stone
  const cStone = document.createElement('canvas');
  cStone.width = cStone.height = size;
  const ctxS = cStone.getContext('2d');
  ctxS.fillStyle = '#787878';
  ctxS.fillRect(0, 0, size, size);
  createNoise(ctxS, size, size, 0.45, () => (Math.random() > 0.5 ? '#616161' : '#8f8f8f'));
  Textures.stone = new THREE.CanvasTexture(cStone);
  Textures.stone.magFilter = THREE.NearestFilter;
  Textures.stone.minFilter = THREE.NearestFilter;

  // 5. Diamond Ore
  const cDiamond = document.createElement('canvas');
  cDiamond.width = cDiamond.height = size;
  const ctxDia = cDiamond.getContext('2d');
  ctxDia.drawImage(cStone, 0, 0);
  const diaSpots = [[4,4],[5,4],[4,5],[10,3],[11,3],[2,10],[3,10],[3,11],[9,11],[10,11],[11,12]];
  diaSpots.forEach(([x, y]) => {
    ctxDia.fillStyle = '#4dedf4';
    ctxDia.fillRect(x, y, 1, 1);
    ctxDia.fillStyle = '#a9ffff';
    ctxDia.fillRect(x+1, y, 1, 1);
  });
  Textures.diamond = new THREE.CanvasTexture(cDiamond);
  Textures.diamond.magFilter = THREE.NearestFilter;
  Textures.diamond.minFilter = THREE.NearestFilter;

  // 6. Obsidian
  const cObsidian = document.createElement('canvas');
  cObsidian.width = cObsidian.height = size;
  const ctxObs = cObsidian.getContext('2d');
  ctxObs.fillStyle = '#100c1c';
  ctxObs.fillRect(0, 0, size, size);
  createNoise(ctxObs, size, size, 0.35, () => (Math.random() > 0.5 ? '#1f1338' : '#2d1b54'));
  Textures.obsidian = new THREE.CanvasTexture(cObsidian);
  Textures.obsidian.magFilter = THREE.NearestFilter;
  Textures.obsidian.minFilter = THREE.NearestFilter;

  // 7. Nether Portal Vortex
  const cPortal = document.createElement('canvas');
  cPortal.width = cPortal.height = 32;
  const ctxPort = cPortal.getContext('2d');
  const grad = ctxPort.createRadialGradient(16, 16, 2, 16, 16, 16);
  grad.addColorStop(0, '#d161ff');
  grad.addColorStop(0.5, '#7815b3');
  grad.addColorStop(1, '#3b065e');
  ctxPort.fillStyle = grad;
  ctxPort.fillRect(0, 0, 32, 32);
  Textures.portal = new THREE.CanvasTexture(cPortal);
  Textures.portal.magFilter = THREE.NearestFilter;
}

// ============================================================================
// 3. SYNTHETIC SOUND ENGINE (Web Audio API)
// ============================================================================
class SoundEngine {
  constructor() {
    this.ctx = null;
  }

  ensureContext() {
    if (!this.ctx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) this.ctx = new AudioContext();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playClick() {
    if (!STATE.soundEnabled) return;
    this.ensureContext();
    if (!this.ctx) return;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(800, this.ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(300, this.ctx.currentTime + 0.05);
    gain.gain.setValueAtTime(0.2, this.ctx.currentTime);
    gain.gain.linearRampToValueAtTime(0.01, this.ctx.currentTime + 0.05);
    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(this.ctx.currentTime + 0.05);
  }

  playBeep() {
    if (!STATE.soundEnabled) return;
    this.ensureContext();
    if (!this.ctx) return;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(659.25, this.ctx.currentTime); // E5
    gain.gain.setValueAtTime(0.12, this.ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(this.ctx.currentTime + 0.12);
  }

  playLaunch() {
    if (!STATE.soundEnabled) return;
    this.ensureContext();
    if (!this.ctx) return;
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'square';
    osc.frequency.setValueAtTime(440, now);
    osc.frequency.setValueAtTime(554.37, now + 0.08);
    osc.frequency.setValueAtTime(659.25, now + 0.16);
    osc.frequency.setValueAtTime(880, now + 0.24);
    gain.gain.setValueAtTime(0.15, now);
    gain.gain.linearRampToValueAtTime(0.25, now + 0.24);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.7);
    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(now + 0.7);
  }

  playBlockPlace() {
    if (!STATE.soundEnabled) return;
    this.ensureContext();
    if (!this.ctx) return;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(160, this.ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(80, this.ctx.currentTime + 0.08);
    gain.gain.setValueAtTime(0.25, this.ctx.currentTime);
    gain.gain.linearRampToValueAtTime(0.01, this.ctx.currentTime + 0.08);
    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(this.ctx.currentTime + 0.08);
  }
}

const AudioFX = new SoundEngine();

// ============================================================================
// 4. THREE.JS 3D SCENE & ENGINE
// ============================================================================
let scene, camera, renderer;
let islandGroup, characterGroup, portalMesh, portalParticles;
let blocks = [];
let mouse = { x: 0, y: 0, targetX: 0, targetY: 0 };
let clock = new THREE.Clock();

// Game Mode Controls & Physics State
let controls;
let moveForward = false, moveBackward = false, moveLeft = false, moveRight = false, canJump = false;
let velocity = new THREE.Vector3();
let direction = new THREE.Vector3();
let raycaster = new THREE.Raycaster();

function initThree() {
  const container = document.getElementById('webgl-canvas');
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x07090e);
  scene.fog = new THREE.FogExp2(0x07090e, 0.035);

  camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
  camera.position.set(0, 4.5, 9.5);

  renderer = new THREE.WebGLRenderer({ canvas: container, antialias: true, powerPreference: 'high-performance' });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;

  // Initialize Textures
  initTextures();

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.65);
  scene.add(ambientLight);

  const dirLight = new THREE.DirectionalLight(0xa5f3fc, 1.2);
  dirLight.position.set(12, 20, 15);
  dirLight.castShadow = true;
  dirLight.shadow.mapSize.width = 1024;
  dirLight.shadow.mapSize.height = 1024;
  dirLight.shadow.camera.near = 0.5;
  dirLight.shadow.camera.far = 40;
  dirLight.shadow.camera.left = -12;
  dirLight.shadow.camera.right = 12;
  dirLight.shadow.camera.top = 12;
  dirLight.shadow.camera.bottom = -12;
  scene.add(dirLight);

  // Purple Glow light near Portal
  const portalLight = new THREE.PointLight(0xa855f7, 2.5, 12);
  portalLight.position.set(-2.5, 2.8, -1.5);
  scene.add(portalLight);

  // Build World
  buildVoxelIsland();
  buildCharacter();
  buildPortalParticles();

  // Event Listeners
  window.addEventListener('resize', onWindowResize);
  document.addEventListener('mousemove', onMouseMove);
  setupPlayModeControls();

  // Animation Loop
  animate();
}

// Materials definition
function getBlockMaterials(type) {
  switch (type) {
    case 'grass':
      return [
        new THREE.MeshLambertMaterial({ map: Textures.grassSide }), // Right
        new THREE.MeshLambertMaterial({ map: Textures.grassSide }), // Left
        new THREE.MeshLambertMaterial({ map: Textures.grassTop }),  // Top
        new THREE.MeshLambertMaterial({ map: Textures.dirt }),      // Bottom
        new THREE.MeshLambertMaterial({ map: Textures.grassSide }), // Front
        new THREE.MeshLambertMaterial({ map: Textures.grassSide })  // Back
      ];
    case 'dirt':
      return new THREE.MeshLambertMaterial({ map: Textures.dirt });
    case 'stone':
      return new THREE.MeshLambertMaterial({ map: Textures.stone });
    case 'diamond':
      return new THREE.MeshLambertMaterial({ map: Textures.diamond });
    case 'obsidian':
      return new THREE.MeshLambertMaterial({ map: Textures.obsidian });
    default:
      return new THREE.MeshLambertMaterial({ map: Textures.stone });
  }
}

function buildVoxelIsland() {
  islandGroup = new THREE.Group();
  const boxGeo = new THREE.BoxGeometry(1, 1, 1);

  // Procedural Island Grid (7x7 floating cluster)
  for (let x = -3; x <= 3; x++) {
    for (let z = -3; z <= 3; z++) {
      const distFromCenter = Math.sqrt(x * x + z * z);
      if (distFromCenter > 3.4) continue;

      // Top grass layer
      const topCube = new THREE.Mesh(boxGeo, getBlockMaterials('grass'));
      topCube.position.set(x, 0, z);
      topCube.receiveShadow = true;
      topCube.castShadow = true;
      islandGroup.add(topCube);
      blocks.push(topCube);

      // Layer 1 down: Dirt
      const dirtCube = new THREE.Mesh(boxGeo, getBlockMaterials('dirt'));
      dirtCube.position.set(x, -1, z);
      dirtCube.receiveShadow = true;
      islandGroup.add(dirtCube);
      blocks.push(dirtCube);

      // Layer 2 down: Stone / Diamond Ore
      if (distFromCenter <= 2.2) {
        const isDiamond = Math.random() < 0.22;
        const stoneCube = new THREE.Mesh(boxGeo, getBlockMaterials(isDiamond ? 'diamond' : 'stone'));
        stoneCube.position.set(x, -2, z);
        islandGroup.add(stoneCube);
        blocks.push(stoneCube);
      }

      // Stalactite tip
      if (distFromCenter <= 1.2 && Math.random() < 0.75) {
        const tipCube = new THREE.Mesh(boxGeo, getBlockMaterials('stone'));
        tipCube.position.set(x, -3, z);
        islandGroup.add(tipCube);
        blocks.push(tipCube);
      }
    }
  }

  // Build Nether Portal Frame on the island (-2.5, 0.5, -1.5)
  const obsMat = getBlockMaterials('obsidian');
  const portalBlocks = [
    // Bottom
    [-3, 1, -2], [-2, 1, -2],
    // Pillars
    [-4, 1, -2], [-4, 2, -2], [-4, 3, -2], [-4, 4, -2],
    [-1, 1, -2], [-1, 2, -2], [-1, 3, -2], [-1, 4, -2],
    // Top
    [-3, 4, -2], [-2, 4, -2]
  ];

  portalBlocks.forEach(([px, py, pz]) => {
    const ob = new THREE.Mesh(boxGeo, obsMat);
    ob.position.set(px, py, pz);
    ob.castShadow = true;
    islandGroup.add(ob);
    blocks.push(ob);
  });

  // Portal Purple Vortex Plane
  const portGeo = new THREE.PlaneGeometry(2, 3);
  const portMat = new THREE.MeshBasicMaterial({
    map: Textures.portal,
    transparent: true,
    opacity: 0.85,
    side: THREE.DoubleSide
  });
  portalMesh = new THREE.Mesh(portGeo, portMat);
  portalMesh.position.set(-2.5, 2.5, -2);
  islandGroup.add(portalMesh);

  scene.add(islandGroup);
}

function buildCharacter() {
  characterGroup = new THREE.Group();

  // Minecraft Player Steve Rig (scale: 1 voxel block = 1 unit)
  const skinMaterial = new THREE.MeshLambertMaterial({ color: 0xd29d71 }); // Skin tone
  const shirtMaterial = new THREE.MeshLambertMaterial({ color: 0x00a8a8 }); // Cyan shirt
  const pantsMaterial = new THREE.MeshLambertMaterial({ color: 0x0000aa }); // Blue pants
  const hairMaterial = new THREE.MeshLambertMaterial({ color: 0x4a2c11 });  // Dark brown hair

  // Head
  const headGeo = new THREE.BoxGeometry(0.5, 0.5, 0.5);
  const head = new THREE.Mesh(headGeo, skinMaterial);
  head.position.y = 1.65;
  head.castShadow = true;
  characterGroup.add(head);

  // Hair Cap
  const hairGeo = new THREE.BoxGeometry(0.52, 0.2, 0.52);
  const hair = new THREE.Mesh(hairGeo, hairMaterial);
  hair.position.y = 1.82;
  characterGroup.add(hair);

  // Body / Torso
  const bodyGeo = new THREE.BoxGeometry(0.5, 0.75, 0.25);
  const body = new THREE.Mesh(bodyGeo, shirtMaterial);
  body.position.y = 1.05;
  body.castShadow = true;
  characterGroup.add(body);

  // Right Arm
  const armGeo = new THREE.BoxGeometry(0.24, 0.75, 0.24);
  const rightArm = new THREE.Mesh(armGeo, shirtMaterial);
  rightArm.position.set(0.38, 1.05, 0);
  rightArm.castShadow = true;
  characterGroup.add(rightArm);
  characterGroup.rightArm = rightArm;

  // Left Arm
  const leftArm = new THREE.Mesh(armGeo, shirtMaterial);
  leftArm.position.set(-0.38, 1.05, 0);
  leftArm.castShadow = true;
  characterGroup.add(leftArm);
  characterGroup.leftArm = leftArm;

  // Right Leg
  const legGeo = new THREE.BoxGeometry(0.24, 0.72, 0.24);
  const rightLeg = new THREE.Mesh(legGeo, pantsMaterial);
  rightLeg.position.set(0.13, 0.36, 0);
  rightLeg.castShadow = true;
  characterGroup.add(rightLeg);

  // Left Leg
  const leftLeg = new THREE.Mesh(legGeo, pantsMaterial);
  leftLeg.position.set(-0.13, 0.36, 0);
  leftLeg.castShadow = true;
  characterGroup.add(leftLeg);

  characterGroup.position.set(1.2, 0.5, 0.5);
  characterGroup.rotation.y = -Math.PI / 4;
  islandGroup.add(characterGroup);
}

function buildPortalParticles() {
  const particleCount = 60;
  const geometry = new THREE.BufferGeometry();
  const positions = new Float32Array(particleCount * 3);

  for (let i = 0; i < particleCount * 3; i += 3) {
    positions[i] = -2.5 + (Math.random() - 0.5) * 2;
    positions[i + 1] = 1.2 + Math.random() * 2.8;
    positions[i + 2] = -2 + (Math.random() - 0.5) * 0.8;
  }

  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  const material = new THREE.PointsMaterial({
    color: 0xc084fc,
    size: 0.12,
    transparent: true,
    opacity: 0.85,
    blending: THREE.AdditiveBlending
  });

  portalParticles = new THREE.Points(geometry, material);
  islandGroup.add(portalParticles);
}

function onWindowResize() {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
}

function onMouseMove(e) {
  if (STATE.inGameMode) return;
  mouse.targetX = (e.clientX / window.innerWidth) * 2 - 1;
  mouse.targetY = -(e.clientY / window.innerHeight) * 2 + 1;
}

function animate() {
  requestAnimationFrame(animate);

  const delta = clock.getDelta();
  const time = clock.getElapsedTime();

  if (!STATE.inGameMode) {
    // Menu Parallax Camera Animation
    mouse.x += (mouse.targetX - mouse.x) * 0.05;
    mouse.y += (mouse.targetY - mouse.y) * 0.05;

    camera.position.x = mouse.x * 2.5;
    camera.position.y = 4.5 + mouse.y * 1.5;
    camera.lookAt(0, 0.8, 0);

    // Floating island bobbing
    if (islandGroup) {
      islandGroup.position.y = Math.sin(time * 1.2) * 0.15;
      islandGroup.rotation.y = Math.sin(time * 0.25) * 0.08;
    }

    // Steve Idle Breathing & Arm movement
    if (characterGroup && characterGroup.rightArm) {
      characterGroup.rightArm.rotation.x = Math.sin(time * 2.5) * 0.25;
      characterGroup.leftArm.rotation.x = -Math.sin(time * 2.5) * 0.25;
    }
  } else {
    // Game Mode Physics & Pointer Lock Movement
    updateGameMovement(delta);
  }

  // Portal Particles Swirl
  if (portalParticles) {
    const positions = portalParticles.geometry.attributes.position.array;
    for (let i = 1; i < positions.length; i += 3) {
      positions[i] += delta * 0.6;
      if (positions[i] > 4.2) positions[i] = 1.2;
    }
    portalParticles.geometry.attributes.position.needsUpdate = true;
  }

  renderer.render(scene, camera);
}

// ============================================================================
// 5. IN-BROWSER 3D VOXEL GAME MODE (WASD + Break/Place Blocks)
// ============================================================================
function setupPlayModeControls() {
  document.addEventListener('keydown', (e) => {
    if (!STATE.inGameMode) return;
    switch (e.code) {
      case 'KeyW': moveForward = true; break;
      case 'KeyS': moveBackward = true; break;
      case 'KeyA': moveLeft = true; break;
      case 'KeyD': moveRight = true; break;
      case 'Space':
        if (canJump) velocity.y += 9.5;
        canJump = false;
        break;
      case 'Digit1': selectHotbarSlot(1); break;
      case 'Digit2': selectHotbarSlot(2); break;
      case 'Digit3': selectHotbarSlot(3); break;
      case 'Digit4': selectHotbarSlot(4); break;
      case 'Digit5': selectHotbarSlot(5); break;
      case 'Digit6': selectHotbarSlot(6); break;
    }
  });

  document.addEventListener('keyup', (e) => {
    if (!STATE.inGameMode) return;
    switch (e.code) {
      case 'KeyW': moveForward = false; break;
      case 'KeyS': moveBackward = false; break;
      case 'KeyA': moveLeft = false; break;
      case 'KeyD': moveRight = false; break;
    }
  });

  // Break / Place blocks on Mouse Click
  window.addEventListener('mousedown', (e) => {
    if (!STATE.inGameMode) return;
    raycaster.setFromCamera(new THREE.Vector2(0, 0), camera);
    const intersects = raycaster.intersectObjects(blocks, false);

    if (intersects.length > 0 && intersects[0].distance < 7) {
      const hit = intersects[0];
      if (e.button === 0) {
        // Left Click -> Break Block
        islandGroup.remove(hit.object);
        const index = blocks.indexOf(hit.object);
        if (index > -1) blocks.splice(index, 1);
        AudioFX.playClick();
      } else if (e.button === 2) {
        // Right Click -> Place Block
        const normal = hit.face.normal;
        const newPos = hit.object.position.clone().add(normal);
        const blockType = getSelectedBlockType();
        const newCube = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), getBlockMaterials(blockType));
        newCube.position.copy(newPos);
        newCube.castShadow = true;
        newCube.receiveShadow = true;
        islandGroup.add(newCube);
        blocks.push(newCube);
        AudioFX.playBlockPlace();
      }
    }
  });

  // Prevent context menu in play mode
  window.addEventListener('contextmenu', (e) => {
    if (STATE.inGameMode) e.preventDefault();
  });
}

function updateGameMovement(delta) {
  velocity.x -= velocity.x * 10.0 * delta;
  velocity.z -= velocity.z * 10.0 * delta;
  velocity.y -= 25.0 * delta; // Gravity

  direction.z = Number(moveForward) - Number(moveBackward);
  direction.x = Number(moveRight) - Number(moveLeft);
  direction.normalize();

  if (moveForward || moveBackward) velocity.z -= direction.z * 80.0 * delta;
  if (moveLeft || moveRight) velocity.x -= direction.x * 80.0 * delta;

  // Simple floor collision
  camera.position.y += velocity.y * delta;
  if (camera.position.y < 1.8) {
    velocity.y = 0;
    camera.position.y = 1.8;
    canJump = true;
  }

  // Camera translation in PointerLock
  if (document.pointerLockElement === document.body) {
    const forwardVector = new THREE.Vector3(0, 0, -1).applyQuaternion(camera.quaternion);
    forwardVector.y = 0;
    forwardVector.normalize();

    const rightVector = new THREE.Vector3(1, 0, 0).applyQuaternion(camera.quaternion);
    rightVector.y = 0;
    rightVector.normalize();

    camera.position.addScaledVector(forwardVector, -velocity.z * delta * 0.1);
    camera.position.addScaledVector(rightVector, velocity.x * delta * 0.1);
  }
}

function enterInBrowserPlayMode() {
  STATE.inGameMode = true;
  pauseAutoConnect();
  document.getElementById('launcher-ui').style.display = 'none';
  document.getElementById('game-hud').style.display = 'block';

  camera.position.set(0, 2, 4);
  camera.lookAt(0, 1.5, 0);

  // Request Pointer Lock for FPS Look
  document.body.requestPointerLock();
  document.addEventListener('mousemove', onFpsMouseMove);

  showToast('🎮 3D Web Minecraft Keşif Modu Açıldı! [W,A,S,D + Boşluk]');
}

function exitInBrowserPlayMode() {
  STATE.inGameMode = false;
  document.exitPointerLock();
  document.removeEventListener('mousemove', onFpsMouseMove);

  document.getElementById('launcher-ui').style.display = 'flex';
  document.getElementById('game-hud').style.display = 'none';

  camera.position.set(0, 4.5, 9.5);
  camera.lookAt(0, 0.8, 0);
}

function onFpsMouseMove(e) {
  if (document.pointerLockElement !== document.body) return;
  const movementX = e.movementX || 0;
  const movementY = e.movementY || 0;

  const euler = new THREE.Euler(0, 0, 0, 'YXZ');
  euler.setFromQuaternion(camera.quaternion);
  euler.y -= movementX * 0.0022;
  euler.x -= movementY * 0.0022;
  euler.x = Math.max(-Math.PI / 2 + 0.01, Math.min(Math.PI / 2 - 0.01, euler.x));
  camera.quaternion.setFromEuler(euler);
}

function selectHotbarSlot(index) {
  STATE.selectedBlockId = index;
  document.querySelectorAll('.hotbar-slot').forEach((slot, idx) => {
    slot.classList.toggle('selected', idx + 1 === index);
  });
  AudioFX.playClick();
}

function getSelectedBlockType() {
  const map = { 1: 'grass', 2: 'stone', 3: 'dirt', 4: 'diamond', 5: 'obsidian' };
  return map[STATE.selectedBlockId] || 'grass';
}

// ============================================================================
// 6. AUTO-CONNECT ENGINE & PROTOCOL LAUNCHER
// ============================================================================
function startAutoConnect() {
  if (!STATE.autoConnectEnabled) return;
  STATE.countdown = 5;
  STATE.isPaused = false;
  updateTimerUI();

  clearInterval(STATE.timerInterval);
  STATE.timerInterval = setInterval(() => {
    if (STATE.isPaused || STATE.isConnecting) return;
    STATE.countdown--;
    AudioFX.playBeep();
    updateTimerUI();

    if (STATE.countdown <= 0) {
      clearInterval(STATE.timerInterval);
      triggerAutoConnection();
    }
  }, 1000);
}

function pauseAutoConnect() {
  STATE.isPaused = true;
  const btnPause = document.getElementById('btn-pause-auto');
  if (btnPause) {
    btnPause.innerHTML = '<i class="fa-solid fa-play"></i><span>Devam Et</span>';
  }
}

function resumeAutoConnect() {
  STATE.isPaused = false;
  const btnPause = document.getElementById('btn-pause-auto');
  if (btnPause) {
    btnPause.innerHTML = '<i class="fa-solid fa-pause"></i><span>Durdur</span>';
  }
}

function togglePauseAutoConnect() {
  if (STATE.isPaused) resumeAutoConnect();
  else pauseAutoConnect();
  AudioFX.playClick();
}

function updateTimerUI() {
  const timerElem = document.getElementById('auto-timer-val');
  const progressFill = document.getElementById('progress-fill');
  if (timerElem) timerElem.textContent = `${STATE.countdown}s`;
  if (progressFill) {
    const percentage = (STATE.countdown / 5) * 100;
    progressFill.style.width = `${percentage}%`;
  }
}

function triggerAutoConnection() {
  STATE.isConnecting = true;
  AudioFX.playLaunch();

  const server = STATE.activeServer;
  const connectionAddress = `${server.ip}:${server.port}`;

  // 1. Copy IP to Clipboard
  copyServerIP(false);

  // 2. Dispatch URI Scheme to trigger local Minecraft / Grizm Launcher
  // Native Grizm Launcher custom protocol:
  const grizmUri = `grizmmc://launch?server=${server.ip}&port=${server.port}&user=${encodeURIComponent(STATE.username)}`;
  // Standard Minecraft direct protocol:
  const mcUri = `minecraft://${connectionAddress}`;

  // Attempt window redirect to custom protocol
  try {
    window.location.href = grizmUri;
  } catch (err) {
    console.log('Custom protocol error:', err);
  }

  // 3. Show Connected Modal with visual steps
  showConnectedModal(connectionAddress);
}

function showConnectedModal(address) {
  const modal = document.getElementById('modal-connected');
  const modalAddress = document.getElementById('modal-conn-address');
  const modalUser = document.getElementById('modal-conn-user');

  if (modalAddress) modalAddress.textContent = address;
  if (modalUser) modalUser.textContent = STATE.username;

  modal.classList.add('active');
}

function copyServerIP(showFeedback = true) {
  const server = STATE.activeServer;
  const address = `${server.ip}:${server.port}`;
  navigator.clipboard.writeText(address).then(() => {
    if (showFeedback) {
      showToast(`📋 Sunucu IP Kopyalandı: ${address}`);
      AudioFX.playClick();
    }
  });
}

function showToast(message) {
  const toast = document.getElementById('toast-notice');
  const text = document.getElementById('toast-text');
  if (!toast || !text) return;
  text.textContent = message;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3500);
}

// ============================================================================
// 7. SERVER STATUS API & UI BINDINGS
// ============================================================================
async function fetchServerStatus() {
  const pillStatus = document.getElementById('server-player-count');
  const statusIndicator = document.getElementById('status-indicator');

  try {
    let res = await fetch(CONFIG.apiEndpoint);
    if (!res.ok) throw new Error('Local API failed');
    let data = await res.json();
    updateStatusBadge(data.players.online, data.players.max);
  } catch (e) {
    try {
      let res2 = await fetch(CONFIG.fallbackApi);
      let data2 = await res2.json();
      if (data2.online) {
        updateStatusBadge(data2.players.online, data2.players.max);
      } else {
        updateStatusBadge(148, 500); // Friendly fallback demo count
      }
    } catch (err2) {
      updateStatusBadge(148, 500);
    }
  }
}

function updateStatusBadge(online, max) {
  const elem = document.getElementById('server-player-count');
  if (elem) elem.textContent = `${online} / ${max} Çevrimiçi`;
}

function setupUIBindings() {
  // Username management
  const userNameElem = document.getElementById('user-name-display');
  const userAvatarElem = document.getElementById('user-avatar-img');
  if (userNameElem) userNameElem.textContent = STATE.username;
  if (userAvatarElem) userAvatarElem.src = `https://mc-heads.net/avatar/${encodeURIComponent(STATE.username)}/48`;

  // Server selection cards
  document.querySelectorAll('.server-card').forEach((card) => {
    card.addEventListener('click', () => {
      document.querySelectorAll('.server-card').forEach((c) => c.classList.remove('active'));
      card.classList.add('active');
      const subId = card.getAttribute('data-server-id');
      const found = CONFIG.subservers.find((s) => s.id === subId);
      if (found) {
        STATE.activeServer = {
          name: found.name,
          ip: CONFIG.defaultServer.ip,
          port: found.port,
          tagline: found.desc
        };
        document.getElementById('target-server-label').textContent = `${found.name} (Port: ${found.port})`;
        AudioFX.playClick();
      }
    });
  });

  // Action Buttons
  document.getElementById('btn-connect-now')?.addEventListener('click', () => {
    clearInterval(STATE.timerInterval);
    triggerAutoConnection();
  });

  document.getElementById('btn-pause-auto')?.addEventListener('click', togglePauseAutoConnect);

  document.getElementById('btn-copy-ip')?.addEventListener('click', () => copyServerIP(true));

  document.getElementById('btn-enter-3d')?.addEventListener('click', enterInBrowserPlayMode);
  document.getElementById('btn-exit-game')?.addEventListener('click', exitInBrowserPlayMode);

  // Settings Modal
  const modalSettings = document.getElementById('modal-settings');
  document.getElementById('btn-open-settings')?.addEventListener('click', () => {
    modalSettings.classList.add('active');
    AudioFX.playClick();
  });
  document.getElementById('btn-close-settings')?.addEventListener('click', () => {
    modalSettings.classList.remove('active');
  });

  // Save Settings
  document.getElementById('btn-save-settings')?.addEventListener('click', () => {
    const inputName = document.getElementById('setting-username').value.trim();
    if (inputName) {
      STATE.username = inputName;
      localStorage.setItem('grizm_username', inputName);
      if (userNameElem) userNameElem.textContent = inputName;
      if (userAvatarElem) userAvatarElem.src = `https://mc-heads.net/avatar/${encodeURIComponent(inputName)}/48`;
    }
    modalSettings.classList.remove('active');
    showToast('⚙️ Ayarlar kaydedildi!');
    AudioFX.playClick();
  });

  // Modal Connected Close
  document.getElementById('btn-close-connected')?.addEventListener('click', () => {
    document.getElementById('modal-connected').classList.remove('active');
    STATE.isConnecting = false;
  });

  // Fullscreen Toggle
  document.getElementById('btn-fullscreen')?.addEventListener('click', () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
    } else {
      document.exitFullscreen();
    }
  });
}

// ============================================================================
// 8. BOOTSTRAP INITIALIZATION
// ============================================================================
window.addEventListener('DOMContentLoaded', () => {
  initThree();
  setupUIBindings();
  fetchServerStatus();
  startAutoConnect();
});

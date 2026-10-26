// Renders the Play Store promo video (1920x1080, 30 fps, ~19.6 s) from the real app UI.
//
// The app runs in headless Chromium inside composer.html with sample data and a virtual clock, so
// every CSS animation is stepped frame by frame and each tap lands exactly on a beat of the music.
// Beats come from beats.py (librosa). Steps: decode the music, find the beats, capture frames,
// then mux with ffmpeg. See docs/RELEASING.md for the tools it needs.
//
// Usage: node render.js [--music path] [--out path] [--fps 30]
const { chromium } = require('playwright');
const { execFileSync } = require('child_process');
const http = require('http');
const fs = require('fs');
const os = require('os');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const args = Object.fromEntries(process.argv.slice(2).map((a, i, all) => (a.startsWith('--') ? [a.slice(2), all[i + 1]] : [])).filter((x) => x.length));
const MUSIC = path.resolve(ROOT, args.music || 'promo_music.mp3');
const OUT = path.resolve(ROOT, args.out || 'store/listing/promo-video-1920x1080.mp4');
const FPS = +(args.fps || 30);
const PORT = 8767;
const TMP = path.join(os.tmpdir(), 'dailybudget-promo');

// ---- Timeline --------------------------------------------------------------------------------
// Video time 0 is one bar (4 beats) before the drop; the phone lands on the drop. Beat indexes
// below are indexes into the music's beat list. The track is 112 BPM: a bar lasts ~2.13 s.
const DROP_BEAT = 12;                 // first strong downbeat of the full-energy section
const FIRST_BEAT = DROP_BEAT - 4;     // music beat at video t=0
const END_BEAT = 40;                  // end card (7 bars after the drop)
const DURATION = 19.6;                // seconds; music fades out over the last 1.4 s
const FADE_OUT = 19.15;               // video fade to black starts here

function timeline(beats) {
  const B = (i) => beats[Math.floor(i)] + (i % 1) * (beats[Math.floor(i) + 1] - beats[Math.floor(i)]);
  const T = {
    beats, drop: DROP_BEAT, intro: FIRST_BEAT, tagline: FIRST_BEAT + 2, end: END_BEAT, duration: DURATION, fadeOut: FADE_OUT,
    captions: [
      { beat: 12, html: 'One number: what you can spend <em>today</em>.' },
      { beat: 16, html: 'Log an expense in <em>two taps</em>.' },
      { beat: 20, html: 'Spend less today, <em>tomorrow grows</em>.' },
      { beat: 24, html: 'Every expense, <em>by day</em>.' },
      { beat: 28, html: '<em>Green</em> days. <em>Red</em> days.' },
      { beat: 32, html: 'Dark, AMOLED or <em>light</em>.' },
      { beat: 36, html: 'Free. Offline. <em>No account</em>.' },
    ],
    // Camera: which side of the stage the phone is on, how far it's zoomed and on what (a selector
    // in the app, or nothing for the whole screen). Each keyframe eases in from the state before it.
    camera: [
      { beat: 12, side: 'right', zoom: 1 },
      { beat: 13.5, side: 'right', zoom: 1.5, focus: '#todayLeft' },
      { beat: 15, side: 'right', zoom: 1 },
      { beat: 16.5, side: 'right', zoom: 1.45, focus: '#addForm' },
      { beat: 20, side: 'left', zoom: 1 },
      { beat: 22, side: 'left', zoom: 1.5, focus: '#projection' },
      { beat: 24, side: 'right', zoom: 1 },
      { beat: 28, side: 'left', zoom: 1 },
      { beat: 30, side: 'left', zoom: 1.4, focus: '#calGrid' },
      { beat: 32, side: 'right', zoom: 1 },
      { beat: 36, side: 'left', zoom: 1 },
      { beat: 37.5, side: 'left', zoom: 1.5, focus: '#todayLeft' },
    ],
  };
  // What happens in the app, by beat: taps on beats, typed digits on eighth notes.
  const actions = [
    [16, 'tap', '#addBtn'],
    [17, 'type', '#amountInput', '1'], [17.5, 'type', '#amountInput', '2'], [18, 'type', '#amountInput', '.'],
    [18.5, 'type', '#amountInput', '8'], [19, 'type', '#amountInput', '0'],
    [19.2, 'type', '#noteInput', 'L'], [19.36, 'type', '#noteInput', 'u'], [19.52, 'type', '#noteInput', 'n'],
    [19.68, 'type', '#noteInput', 'c'], [19.84, 'type', '#noteInput', 'h'],
    [20, 'tap', '#addForm button[type=submit]'],
    [24, 'tap', '.tab[data-tab="expenses"]'],
    [28, 'tap', '.tab[data-tab="calendar"]'],
    [30, 'tap', '#calGrid .cal-day.over'],
    [32, 'tap', '#settingsBtn'],
    [33, 'tap', '#rowTheme'],
    [34, 'tap', '#themePicker button[data-theme="light"]'],
    [35, 'tap', '#settingsBack'],
    [36, 'tap', '.tab[data-tab="budget"]'],
  ].map(([beat, kind, sel, text]) => ({ t: B(beat), kind, sel, text }));
  return { T, actions };
}

// ---- Helpers ---------------------------------------------------------------------------------
const TYPES = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.png': 'image/png', '.svg': 'image/svg+xml' };
function serve(root, port) {
  return new Promise((resolve) => {
    const srv = http.createServer((req, res) => {
      const file = path.join(root, decodeURIComponent(req.url.split('?')[0]));
      fs.readFile(file, (err, data) => {
        if (err) { res.writeHead(404); res.end(); return; }
        res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-store' });
        res.end(data);
      });
    });
    srv.listen(port, () => resolve(srv));
  });
}
const run = (cmd, a) => execFileSync(cmd, a, { stdio: ['ignore', 'pipe', 'inherit'] });

(async () => {
  if (!fs.existsSync(MUSIC)) throw new Error('Music not found: ' + MUSIC);
  fs.rmSync(TMP, { recursive: true, force: true });
  fs.mkdirSync(path.join(TMP, 'frames'), { recursive: true });

  // 1. Decode once so the beat analysis and the final cut use the same samples.
  const wav = path.join(TMP, 'music.wav');
  run('ffmpeg', ['-y', '-loglevel', 'error', '-i', MUSIC, '-ac', '2', '-ar', '48000', wav]);

  // 2. Beats.
  const beatsFile = path.join(TMP, 'beats.json');
  run('uv', ['run', '--python', '3.12', '--with', 'librosa', '--with', 'numpy', 'python', '-I', path.join(__dirname, 'beats.py'), wav, beatsFile]);
  const abs = JSON.parse(fs.readFileSync(beatsFile, 'utf8')).beats;
  const beats = abs.map((x) => +(x - abs[FIRST_BEAT]).toFixed(3));
  const { T, actions } = timeline(beats);
  console.log(`tempo grid: ${beats.length} beats, drop at ${beats[DROP_BEAT]} s, ${Math.round(DURATION * FPS)} frames`);

  // 3. Frames.
  const srv = await serve(ROOT, PORT);
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => console.error('page error:', e.message));
  await page.goto(`http://localhost:${PORT}/store/promo/composer.html`);
  await page.frameLocator('#app').locator('#todayLeft').waitFor();
  await page.evaluate(async () => {
    await document.fonts.ready;
    await document.getElementById('app').contentDocument.fonts.ready;
    await new Promise((r) => setTimeout(r, 300));
  });
  await page.evaluate((T) => window.setup(T), T);

  const total = Math.round(DURATION * FPS);
  let next = 0;
  for (let i = 0; i < total; i++) {
    const t = i / FPS;
    while (next < actions.length && actions[next].t <= t + 0.5 / FPS) { // nearest frame
      const a = actions[next++];
      await page.evaluate(({ a }) => (a.kind === 'tap' ? window.tap(a.sel, a.t) : window.type(a.sel, a.text)), { a });
    }
    await page.evaluate((t) => window.renderAt(t), t);
    // Let the iframe's animation events (animationend etc.) dispatch before the screenshot.
    await page.evaluate(() => new Promise((r) => { let n = 0; const f = () => (++n >= 2 ? r() : requestAnimationFrame(f)); requestAnimationFrame(f); setTimeout(r, 80); }));
    await page.screenshot({ path: path.join(TMP, 'frames', `f${String(i).padStart(5, '0')}.png`) });
    if (i % 90 === 0) console.log(`frame ${i}/${total}`);
  }
  await browser.close();
  srv.close();

  // 4. Mux: the music from one bar before the drop, a short fade in and a fade out at the end.
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  run('ffmpeg', ['-y', '-loglevel', 'error',
    '-framerate', String(FPS), '-i', path.join(TMP, 'frames', 'f%05d.png'),
    '-ss', String(abs[FIRST_BEAT]), '-t', String(DURATION), '-i', wav,
    '-af', `afade=t=in:st=0:d=0.15,afade=t=out:st=${(DURATION - 1.4).toFixed(2)}:d=1.4`,
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-profile:v', 'high', '-level', '4.1',
    '-c:a', 'aac', '-b:a', '192k', '-movflags', '+faststart', '-shortest', OUT]);
  console.log('wrote', path.relative(ROOT, OUT));
})().catch((e) => { console.error(e); process.exit(1); });

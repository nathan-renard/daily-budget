# Beat grid of the promo music, for store/promo/render.js: writes {duration, tempo, beats[], kick[], ...} as JSON.
# Usage: uv run --with librosa --with numpy python -I beats.py music.wav beats.json
import sys, json, numpy as np, librosa
y, sr = librosa.load(sys.argv[1], sr=22050, mono=True)
dur = len(y)/sr
tempo, beats = librosa.beat.beat_track(y=y, sr=sr, units='time', trim=False)
onset_env = librosa.onset.onset_strength(y=y, sr=sr)
rms = librosa.feature.rms(y=y)[0]
t_rms = librosa.frames_to_time(np.arange(len(rms)), sr=sr)
# energy per second
sec = np.arange(0, int(dur))
e = [float(rms[(t_rms>=s)&(t_rms<s+1)].mean()) for s in sec]
# downbeat-ish: onset strength at beats
ob = librosa.onset.onset_strength(y=y, sr=sr)
bt = librosa.frames_to_time(np.arange(len(ob)), sr=sr)
strength = [float(ob[np.argmin(np.abs(bt-b))]) for b in beats]
# kick per beat, 0-1: energy under 150 Hz just around the beat, relative to the loudest beat (square
# root, so quiet intro hits still show). Breaks where the kick and bass drop out come out near 0.
S = np.abs(librosa.stft(y))
low = S[librosa.fft_frequencies(sr=sr) < 150].sum(0)
tl = librosa.frames_to_time(np.arange(len(low)), sr=sr)
kick = np.array([low[(tl >= b-.04) & (tl < b+.08)].mean() for b in beats])
kick = np.sqrt(kick / kick.max())
out = {"duration": dur, "tempo": float(np.atleast_1d(tempo)[0]), "beats": [round(float(b),3) for b in beats],
       "beat_strength": [round(s,2) for s in strength], "kick": [round(float(k),2) for k in kick],
       "energy_per_sec": [round(x,4) for x in e]}
json.dump(out, open(sys.argv[2],'w'))
print("tempo", out["tempo"], "dur", dur, "nbeats", len(beats))
print("beats[:40]", out["beats"][:40])
print("kick[:48]", out["kick"][:48])
print("energy", [round(x,3) for x in e])

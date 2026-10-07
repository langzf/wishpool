"""Capture real Android frames around the T33 debug unlock.
Requires adb on PATH and requests the debug endpoint with curl.exe.
Pillow is optional; when installed a contact sheet is written.
"""
import argparse, pathlib, subprocess, time, json

def run(*args):
    return subprocess.run(args, check=True, text=True, capture_output=True).stdout

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--wish-id', required=True); p.add_argument('--base-url', default='http://localhost:18080')
    p.add_argument('--internal-token', default='wishpool-local-internal-token'); p.add_argument('--out', default='t33-evidence')
    a = p.parse_args(); out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    headers = ['-H', 'X-Internal-Token: ' + a.internal_token]
    state = run('curl.exe', '-sS', *headers, f'{a.base_url}/internal/debug/wishes/{a.wish_id}')
    (out/'before.json').write_text(state, encoding='utf-8')
    run('curl.exe', '-sS', '-X', 'POST', *headers, f'{a.base_url}/internal/debug/wishes/{a.wish_id}/unlock')
    frames=[]
    for i in range(10):
        path = out / f'frame-{i:02d}-{int(time.time()*1000)}.png'
        subprocess.run(['adb','shell','screencap','-p','/sdcard/t33.png'], check=True)
        subprocess.run(['adb','pull','/sdcard/t33.png',str(path)], check=True, stdout=subprocess.DEVNULL)
        frames.append(str(path)); time.sleep(.2)
    (out/'frames.json').write_text(json.dumps(frames, ensure_ascii=False, indent=2), encoding='utf-8')
    try:
        from PIL import Image, ImageDraw
        imgs=[Image.open(x).convert('RGB') for x in frames]; thumb=(360,640)
        sheet=Image.new('RGB',(thumb[0]*5,thumb[1]*2),'white')
        for i,img in enumerate(imgs):
            img.thumbnail(thumb); x=(i%5)*thumb[0]; y=(i//5)*thumb[1]; sheet.paste(img,(x,y))
            ImageDraw.Draw(sheet).text((x+8,y+8),str(i),fill='red')
        sheet.save(out/'contact-sheet.png')
    except ImportError:
        print('Pillow not installed; frames remain valid, contact sheet not generated.')
    print(f'Captured {len(frames)} real-device frames in {out}')
if __name__ == '__main__': main()

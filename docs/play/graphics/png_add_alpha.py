"""Rewrites an 8-bit RGB PNG as RGBA with a fully opaque alpha channel."""
import struct, sys, zlib

def chunks(data):
    pos = 8
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        yield kind, data[pos + 8:pos + 8 + length]
        pos += 12 + length

def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)

src, dst = sys.argv[1], sys.argv[2]
data = open(src, 'rb').read()
idat = b''
for kind, body in chunks(data):
    if kind == b'IHDR':
        w, h, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', body)
        assert depth == 8 and ctype == 2 and interlace == 0, (depth, ctype, interlace)
    elif kind == b'IDAT':
        idat += body
raw = zlib.decompress(idat)
bpp, stride = 3, w * 3
prev = bytearray(stride)
out = bytearray()
pos = 0
for _ in range(h):
    f = raw[pos]; line = bytearray(raw[pos + 1:pos + 1 + stride]); pos += 1 + stride
    for i in range(stride):
        a = line[i - bpp] if i >= bpp else 0
        b = prev[i]
        c = prev[i - bpp] if i >= bpp else 0
        if f == 1: line[i] = (line[i] + a) & 255
        elif f == 2: line[i] = (line[i] + b) & 255
        elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
        elif f == 4: line[i] = (line[i] + paeth(a, b, c)) & 255
    out.append(0)
    for i in range(0, stride, 3):
        out += line[i:i + 3] + b'\xff'
    prev = line

def chunk(kind, body):
    return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)

png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
    + chunk(b'IDAT', zlib.compress(bytes(out), 9)) + chunk(b'IEND', b'')
open(dst, 'wb').write(png)
print(dst, w, h, 'RGBA')

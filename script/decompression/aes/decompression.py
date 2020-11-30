import sys
import os
import base64
import pyexiv2
from PIL import Image
from Crypto.Cipher import AES


def decrypt_cbc(secure, passw):
    iv = 16 * b'\0'
    aes = AES.new(passw, AES.MODE_CBC, iv)
    dec = aes.decrypt(secure)
    return dec

def decrypt_cfb(secure, passw):
    iv = 16 * b'\0'
    aes = AES.new(passw, AES.MODE_CFB, iv)
    dec = aes.decrypt(secure)
    return dec

def decrypt_ecb(secure, passw):
    aes = AES.new(passw, AES.MODE_ECB)
    dec = aes.decrypt(secure)
    return dec

def decrypt_ofb(secure, passw):
    iv = 16 * b'\0'
    aes = AES.new(passw, AES.MODE_OFB, iv)
    dec = aes.decrypt(secure)
    return dec

def checkCipher(cipher, mode):
    if cipher == 'aes':
        if mode == 'cbc':
            return 0
        else:
            return -3
    else:
        if mode != 'cbc':
            return -4
        return -2

file_name = sys.argv[1]
cipher = sys.argv[2]
mode = sys.argv[3]
password = sys.argv[4]
root_path= sys.argv[5]

# Booleano per la gestione dell'eccezzione relativo alla password
flag = False

# Allungo stringa contenente la password in un multiplo di 16 (per AES)
password += ((16 - len(password) % 16) * "0")

decrompressed_path = root_path+"/data/imgs/out/decompressed"
secure_path = root_path+"/data/imgs/out/secure"
file_path = root_path+"/data/file/secret.txt"
# Apro l'immagine sicura
img_secure = Image.open(secure_path + "/secure.png")

# Creo la nuova immagine che andrà nella cartella decompressed
img_decompressed = img_secure.copy()

f=open(file_path,"r")
exif=f.readline()

iptc=""
for line in f:
    iptc=iptc+line

f.close()

rois=iptc.split("separator")
info=exif.split("-")

for count in range(0, len(rois)):

    # Slitto le informazioni
    info_parts = info[count].split(",")
    lenght_roi = info_parts[0]
    x = info_parts[2]
    y = info_parts[3]

    # Decodifico la stringa con dentro i bytes della ROI
    img_decoded = base64.b64decode(rois[count])

    # Decritto il ROI con la password e elimino l'allungamento che avevo fatto in precedenza per effettuare AES
    if mode=='cbc':
        img_decrypted_decoded = decrypt_cbc(img_decoded, password)[:int(lenght_roi)]
    if mode=='cfb':
        img_decrypted_decoded = decrypt_cfb(img_decoded, password)[:int(lenght_roi)]
    if mode=='ecb':
        img_decrypted_decoded = decrypt_ecb(img_decoded, password)[:int(lenght_roi)]
    if mode=='ofb':
        img_decrypted_decoded = decrypt_ofb(img_decoded, password)[:int(lenght_roi)]


    # Decodifico il ROI ottenuto in precedenza
    img_decrypted_decoded = base64.b64decode(img_decrypted_decoded)

    # Creo un file temporaneo relativo al ROI
    with open(decrompressed_path + "/tmp.jpg", "wb") as img:
        img.write(img_decrypted_decoded)
        img.close()

    # Gestione eccezione per la password errata
    try:
        roi = Image.open(decrompressed_path + "/tmp.jpg")

        # Incollo i bytes decoficati delle ROI all'interno dell'immagine decompressa
        img_decompressed.paste(roi, (int(x), int(y)))
    except Exception:
        flag = True

        # Rimuovo il file temporaneo salvato in precedenza
        os.remove(decrompressed_path + "/tmp.jpg")

if flag is False:
    # Salvo la nuova immagine decompressa
    img_decompressed.save(decrompressed_path + "/decompressed.jpg", format='JPEG', quality=100)

    # Rimuovo il file temporaneo salvato in precedenza
    os.remove(decrompressed_path + "/tmp.jpg")

# Ritorni per vedere se la decompressione ha avuto successo
# 0 decrompessione effettuata con successo
# -1 password per AES errata
# -2 se il cifrario non è corretto
# -3 se la modalità non è corretta
# -4 se sia il cifrario che la modalità non sono corretti
if flag is True:
    print("-1")
else:
    value = checkCipher(cipher, mode)
    if value == -2:
        print("-2")
    elif value == -3:
        print("-3")
    elif value == -4:
        print("-4")
    else:
        print("0")


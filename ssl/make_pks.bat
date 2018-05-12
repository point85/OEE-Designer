set RANDFILE=C:\tools\OpenSSL-Win32h\.rnd
C:\tools\OpenSSL-Win32h\bin\openssl.exe pkcs12 -export -name opcua -in out/certificate.crt -inkey out/privateKey.key > out/point85.p12
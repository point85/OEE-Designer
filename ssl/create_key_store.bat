rem Create X509 certificate with Basic256Sha256
call openssl req -x509 -sha256 -newkey rsa:2048 -keyout privateKey.key -out certificate.crt -extensions v3_self_signed -config openssl.cnf
rem Create one file from certificate and key
set RANDFILE=C:\tools\openssl\.rnd
openssl pkcs12 -export -name opcua -in certificate.crt -inkey privateKey.key > opcua.p12
rem Create the keystore
C:\jdk\jdk1.8.0_152-64\jre\bin\keytool -importkeystore -srckeystore opcua.p12 -destkeystore opcua.keystore -srcstoretype pkcs12 -alias opcua
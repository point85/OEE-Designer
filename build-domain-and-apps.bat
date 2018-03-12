rem build both domain and apps
call c:/dev/OEE-Domain/install-oee-domain-jar.bat
call ant -f ./fxbuild/build.xml build-all
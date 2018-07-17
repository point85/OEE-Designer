rem build both domain and apps
cd ../OEE-Domain
call install-oee-domain-jar.bat
cd ../OEE-Designer
call ant -f ./fxbuild/build.xml build-all
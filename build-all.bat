echo on
rem set JAVA_HOME = C:\jdk\jdk1.8.0_202-64
rem Windows shell script to build all of the Point85 projects
call mvn --version
rem (1) Domain
cd ../OEE-Domain
call install-oee-domain-jar.bat
rem (2) JFX applications
cd ../OEE-Designer
call build-jfx-app.bat
rem (3) Collector
cd ../OEE-Collector
call build-collector.bat
rem (4) Distribution zip
cd ../OEE-Designer
call build-distro.bat
rem (5) Operations
cd ../OEE-Operations
call build-operator.bat
cd ../OEE-Designer
rem Build Finished
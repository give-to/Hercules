mvn install:install-file \
  -DgroupId=org.example \
  -DartifactId=antlr \
  -Dversion=4.9.2 \
  -Dpackaging=jar \
  -Dfile=lib/antlr-runtime-4.9.2.jar

mvn install:install-file \
  -DgroupId=org.example \
  -DartifactId=approxlib \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -Dfile=lib/approxlib_v1.0.jar

mvn install:install-file \
  -DgroupId=org.example \
  -DartifactId=TBar \
  -Dversion=0.0.1 \
  -Dpackaging=jar \
  -Dfile=lib/TBar-0.0.1-SNAPSHOT.jar

mvn install:install-file \
  -DgroupId=org.example \
  -DartifactId=jfxrt \
  -Dversion=0.0.1 \
  -Dpackaging=jar \
  -Dfile=lib/jfxrt.jar

mvn assembly:assembly
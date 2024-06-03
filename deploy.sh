mvn deploy:deploy-file -DgroupId=org.variantsync -DartifactId=trace-boosting -Dversion=0.1.0 -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=./target/trace-boosting-1.0-jar-with-dependencies.jar


# GitHub preparation

Publish this project folder as its own repository; the outer workspace is not the project. Publishing has not been performed.

```sh
git init
git add .gitignore pom.xml config database src/main/resources
git commit -m "build: establish Maven project and relational schema"
git add src/main/java/com/railway/model src/main/java/com/railway/database src/main/java/com/railway/repository src/main/java/com/railway/service src/main/java/com/railway/utils
git commit -m "feat: implement transactional reservation services"
git add src/main/java/com/railway/controller src/main/java/com/railway/view src/main/java/com/railway/App.java
git commit -m "feat: add passenger and admin Swing workflows"
git add src/test .github
git commit -m "test: cover permissions booking concurrency and refunds"
git add README.md docs
git commit -m "docs: explain architecture testing and coursework design"
```

These are suggested groupings, not a fabricated development history. Future commits should explain why behavior changes. Check git status before pushing; credentials, data/ and target/ must be absent. Choose a license and add your author/course information before public release.

Suggested name: train-ticket-reservation-java. Topics: java, swing, mysql, jdbc, oop, mvc, junit.

Create an empty GitHub repository, add its URL as origin and push when ready. Verify the supplied workflow before claiming MySQL CI passed. Attach the JAR to a release instead of committing binaries.

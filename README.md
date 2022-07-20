# compare-uuid-vs-long

Comparing DB performance of UUID vs long

This small applcation was made to test and compare performance differences between different 
types of primary keys in a database. 

The test compares normal Id (long) and two variants that include a UUID (string).
Raw UUID and UUID with a prefix.

The application can generate a DB tables and data to test and then picking random data measures the
time needed to retrieve the data 100 times.

# More detaild run through

The application creates 6 tables. Task and SecurityMember tables for each type which are Long, UUID and Raw UUID.
Task has an id and a UserId which is a foreign key to SecurityMember Id. SecurityMember also has name column.
The security member tables have different types of primary keys.

SecurityMemberLong has normal id using Long.

SecurityMemberRawUuid has a Java generated Uuid which is a string. Looks like this: 79CB0F49-FA8E-48B7-9540-9CCF85135820

SecurityMemberUuid is a UUID combined with a prefix. Here we use a prefix "USER-". 
It would look something like this:
USER-58E7FFE3-07CE-4B72-8E3C-2D1EB6E0E3B2

When creating data the application will first create users with random UUIDs.
Users are limited to 100'000 as our use case will probably not have more than that.

After that the application will get a list of users of lenght ~0.1% of the amonut of tasks you want to create.
From that list it will create tasks for each user randomly which means a user usually has ~1000 tasks.

When comparing the will execute a select query with an inner join 100 times each getting a new random user to query.
The query times are measured and added to a list and an average is calculated at the end.

# Results

Results on a database size of 10'000'000 rows of tasks and 100'000 rows of users on i7-8700k

|                                   | Long  | UUID  | Raw UUID |
|-----------------------------------|-------|-------|----------|
| PostgreSQL                        | 1.688 | 1.901 | 1.701    |
| MariaDB                           | 0.539 | 0.714 | 0.628    |
| MySQL                             | 0.828 | 1.217 | 1.169    |
| MsSQL (parametersAsUnicode=false) | 1.097 | 1.347 | 1.121    |
| OracleDB                          | 6.263 | 6.788 | 6.29     |


Notice that to MsSql "sendStringParametersAsUnicode=False" connection parameter was added.
This is because the MsSql Server converts our parameter in prepared statement to a unicode string which takes
not only a lot of time but also a lot CPU resources.


# How to run

Everything should be executed from the root of the project unless specified otherwise.

**Databases public users**
Go to the build directory and run `docker compose up`.
It will build fresh postgres, mysql, mariadb, mssql and oracle databases in a docker container.

**Databases intern users**
Run `docker-compose up`.
This option will use databases from a private docker registry which already have 100 thousand users and 10 million tasks generated.

The application was coded with Java 17. To build it run `mvn clean install`.
You can start the application by executing `java -jar target/uuid.vd.long-0.0.1-SNAPSHOT.jar`.

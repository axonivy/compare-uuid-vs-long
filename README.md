# compare-uuid-vs-long

Comparing DB performance of UUID vs long

This small applcation was made to test and compare performance differences between different 
types of primary keys in a database. 

The test compares normal Id (long) and two variants that include a UUID (string).
Raw UUID and UUID with a prefix.

The application can generate a DB tables and data to test and then picking random data measures the
time needed to retrieve the data 1000 times.

# More detaild run through

The application creates 4 tables. Task, SecurityMemberLong, SecurityMemberUuid and SecurityMemberRawUuid.
Task has foreign keys to all of the security member tables.
The security member tables have different types of primary keys.
SecurityMemberLong has normal id using Long.
SecurityMemberRawUuid has a Java generated Uuid which is a string. Looks like this: 79CB0F49-FA8E-48B7-9540-9CCF85135820
SecurityMemberUuid is a UUID combined with a prefix. Here we use a prefix "USER-". 
It would look something like this: USER-58E7FFE3-07CE-4B72-8E3C-2D1EB6E0E3B2

When creating data the application will first create users with random UUIDs.
After that the application will create the same amount of tasks.
After every 100 tasks the application choses random Long, Uuid and RawUuid user and creates tasks for them.

When comparing the application will pick one random user for each Id type and search for all tasks of that user 1000 times.
The query times are measured and added to a list and an average is calculated at the end.

# Results

After running comparing 5 times the average times where:

| Table   | 1     | 2     | 3     | 4     | 5     | Average |
|---------|-------|-------|-------|-------|-------|---------|
| Long    | 5,679 | 6,354 | 5,990 | 5,613 | 5,725 | 5,872   |
| Uuid    | 7,134 | 7,877 | 7,255 | 7,098 | 7,069 | 7,287   |
| RawUuid | 7,164 | 7,078 | 6,768 | 7,209 | 6,759 | 6,996   |

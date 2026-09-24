INSERT INTO timetable_release (payload)
SELECT REPLACE(
    payload,
    '"id":0,"startTime":"8:00","predTime":"8:15","schoolArrival":"8:30","runCount":2},{"id":1,"startTime":"8:05","predTime":"8:20","schoolArrival":"8:35","runCount":3}',
    '"id":0,"startTime":"-","predTime":"8:15","schoolArrival":"8:30","runCount":2},{"id":1,"startTime":"-","predTime":"8:20","schoolArrival":"8:35","runCount":3}'
)
FROM timetable_release
WHERE version = (SELECT MAX(version) FROM timetable_release);

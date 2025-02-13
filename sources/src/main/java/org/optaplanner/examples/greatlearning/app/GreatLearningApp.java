package org.optaplanner.examples.greatlearning.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.CourseDateTimeSlotsGenerator;
import org.optaplanner.examples.greatlearning.util.DateTimeSlotsGenerator;
import org.optaplanner.examples.greatlearning.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GreatLearningApp {
    public static void main(String[] args) throws IOException {
        SolverFactory<GLCalendar> solverFactory = SolverFactory.createFromXmlResource(
                "org/optaplanner/examples/greatlearning/solver/glCalendarSolverConfig.xml");
        Solver<GLCalendar> solver = solverFactory.buildSolver();

//        GLCalendar unplannedSolution = generateUnplannedCalendar();
//
//        GLCalendar plannedSolution = solver.solve(unplannedSolution);
//
//        display(plannedSolution);
        GLCalendar unplannedSolution = loadFromFile();
        GLCalendar plannedSolution = solver.solve(unplannedSolution);

        Collections.sort(plannedSolution.getConstraintsBroken());

        for (String constraint : plannedSolution.getConstraintsBroken()) {
            System.out.println(constraint);
        }
        //System.out.println(plannedSolution.getConstraintsBroken());

        displayCalendar(plannedSolution);
        System.out.println("********************");
        display(plannedSolution);
    }

    private static GLCalendar loadFromFile() throws IOException {
        GLCalendar glCalendar = new GLCalendar();

        ObjectMapper objectMapper = new ObjectMapper();
        InputStream resourceAsStream = GreatLearningApp.class.getResourceAsStream("/org/optaplanner/examples/greatlearning/faculty_scheduling_input_Saba_v17.json");
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        JsonNode jsonNode = objectMapper.readTree(testData);
        JsonNode programs = jsonNode.get("programs");

        Iterator<Map.Entry<String, JsonNode>> programIterator = programs.fields();

        Map<String, Program> programMap = new LinkedHashMap<>();
        Map<String, Teacher> teacherMap = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> programCourseIndices = new HashMap<>();

        while (programIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = programIterator.next();
            Program program = new Program();
            String programName = entry.getKey();
            program.setName(programName);

            ArrayNode coursesNode = (ArrayNode) entry.getValue().get("courses");
            List<Course> courseList = new ArrayList<>();
            Map<String, Integer> courseIdxs = new HashMap<>();
            for (int i = 0; i < coursesNode.size(); i++) {
                String courseName = coursesNode.get(i).asText();
                Course course = new Course();
                course.setName(courseName);
                courseList.add(course);
                courseIdxs.put(courseName, i);
            }
            programCourseIndices.put(programName, courseIdxs);
            program.setCourseList(courseList);


            ArrayNode residencyDaysCounts = (ArrayNode) entry.getValue().get("residency_days_count");
            List<Integer> monthlyResidencyDays = new ArrayList<>();
            for (int i = 0; i < residencyDaysCounts.size(); i++) {
                int count = residencyDaysCounts.get(i).asInt();
                monthlyResidencyDays.add(count);
            }
            program.setMonthlyResidencyDays(monthlyResidencyDays);

            programMap.put(programName, program);
        }

        JsonNode locationsNode = jsonNode.get("locations");
        Iterator<Map.Entry<String, JsonNode>> locationsIterator = locationsNode.fields();
        Map<String, Location> locationMap = new HashMap<>();
        while (locationsIterator.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = locationsIterator.next();
            String locationName = nodeEntry.getKey();
            int rooms = nodeEntry.getValue().get("simultaneous_batches").asInt();
            ArrayNode exceptionDays = (ArrayNode) nodeEntry.getValue().get("exception_days");
            List<LocalDate> exceptionDates = new ArrayList<>();

            for (int i = 0; i < exceptionDays.size(); i++) {
                String dateString = exceptionDays.get(i).asText();
                String split[] = dateString.split("/");

                exceptionDates.add(LocalDate.of(Integer.parseInt(split[2]), Integer.parseInt(split[1]), Integer.parseInt(split[0])));
            }
            Location location = new Location();
            location.setName(locationName);
            location.setRooms(rooms);
            location.setHolidays(exceptionDates);

            locationMap.put(locationName, location);
        }

        ArrayNode batchesNode = (ArrayNode) jsonNode.get("batches");
        Map<String, List<Batch>> programBatchMap = new HashMap<>();
        for (int i = 0; i < batchesNode.size(); i++) {
            JsonNode batchNode = batchesNode.get(i);
            String batchName = batchNode.get("name").asText();
            String programName = batchNode.get("program").asText();
            String locationName = batchNode.get("location").asText();

            Location location = locationMap.get(locationName);

            String start_date = batchNode.get("start_date").asText();
            String split[] = start_date.split("/");

            LocalDate startDate = LocalDate.of(Integer.parseInt(split[2]), Integer.parseInt(split[1]), Integer.parseInt(split[0]));
            int minGapBetweenResidency = batchNode.get("min_gap_bw_residencies").asInt();
            int maxGapBetweenResidency = batchNode.get("max_gap_bw_residencies").asInt();
            int maxCourseInFlight = batchNode.get("max_courses_in_flight").asInt(); //TODO

            ArrayNode residencyDaysCounts = (ArrayNode) batchNode.get("residency_days_count");
            List<Integer> monthlyResidencyDays = new ArrayList<>();
            for (int j = 0; j < residencyDaysCounts.size(); j++) {
                int count = residencyDaysCounts.get(j).asInt();
                monthlyResidencyDays.add(count);
            }

            Batch batch = new Batch();
            batch.setMonthlyResidencyDays(monthlyResidencyDays);

            batch.setLocation(location);
            batch.setName(batchName);
            batch.setProgram(null); //set program_name
            batch.setMaxGapBetweenResidenciesInDays(maxGapBetweenResidency);
            batch.setMinGapBetweenResidenciesInDays(minGapBetweenResidency);
            batch.setMaxCoursesInFLight(maxCourseInFlight);
            //max_courses_in_flight
            batch.setStartDate(startDate);
            Program program = programMap.get(programName);
            batch.setProgram(program);

            List<Batch> batches = programBatchMap.get(programName);
            if (batches == null) {
                batches = new ArrayList<>();
            }
            batches.add(batch);
            programBatchMap.put(programName, batches);
        }

        JsonNode coursesNode = jsonNode.get("courses");
        Iterator<Map.Entry<String, JsonNode>> courseIterator = coursesNode.fields();
        Map<String, Course> courseMap = new LinkedHashMap<>();

        while (courseIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = courseIterator.next();
            String courseName = entry.getKey();
            int duration = entry.getValue().get("duration").asInt(); //slotNum = duration/4
            ArrayNode slotsNode = (ArrayNode) entry.getValue().get("slots");
            List<Map.Entry<String, Integer>> slots = new ArrayList<>();
            for (int i = 0; i < slotsNode.size(); i++) {
                slots.add(new AbstractMap.SimpleEntry<>(slotsNode.get(i).get("name").asText(), slotsNode.get(i).get("duration").asInt()));
            }
            Course course = new Course();
            course.setName(courseName);
            course.setSlotsNum(duration / 4);
            course.setSlots(slots);

            courseMap.put(courseName, course);
        }

        JsonNode facultiesNode = jsonNode.get("faculties");
        Iterator<Map.Entry<String, JsonNode>> facultiesIterator = facultiesNode.fields();
        while (facultiesIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = facultiesIterator.next();
            String facultyName = entry.getKey();
            Teacher teacher = new Teacher();
            teacher.setName(facultyName);

            ArrayNode locationsAvailable = (ArrayNode) entry.getValue().get("locations_available");
            Set<String> availableLocations = new HashSet<>();

            for (int i = 0; i < locationsAvailable.size(); i++) {
                String locationName = locationsAvailable.get(i).asText();
                availableLocations.add(locationName);
            }
            teacher.setAvailableLocations(availableLocations);

            ArrayNode restrictedDays = (ArrayNode) entry.getValue().get("not_available_days");
            Set<LocalDate> holidays = new HashSet<>();
            for (int i = 0; i < restrictedDays.size(); i++) {
                String dateText = restrictedDays.get(i).asText();
                String split[] = dateText.split("/");
                LocalDate date = LocalDate.of(Integer.parseInt(split[2]), Integer.parseInt(split[1]), Integer.parseInt(split[0]));
                holidays.add(date);
            }
            teacher.setHolidays(holidays);
            teacherMap.put(facultyName, teacher);
        }

        JsonNode courseFacultiesNode = jsonNode.get("courses_faculties");
        Iterator<Map.Entry<String, JsonNode>> cfIterator = courseFacultiesNode.fields();
        Map<String, List<Teacher>> courseTeacherMap = new HashMap<>();

        while (cfIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = cfIterator.next();
            String courseName = entry.getKey();
            ArrayNode facultyNodes = (ArrayNode) entry.getValue();
            for (int i = 0; i < facultyNodes.size(); i++) {
                String facultyName = facultyNodes.get(i).asText();
                Teacher teacher = teacherMap.get(facultyName);
                if (teacher != null) {
                    Set<String> canTeachCourses = teacher.getCanTeachCourses();
                    if (canTeachCourses == null) {
                        canTeachCourses = new HashSet<>();
                    }
                    canTeachCourses.add(courseName);
                    teacher.setCanTeachCourses(canTeachCourses);

                    List<Teacher> teacherList = courseTeacherMap.get(courseName);
                    if (teacherList == null) {
                        teacherList = new ArrayList<>();
                    }
                    teacherList.add(teacher);
                    courseTeacherMap.put(courseName, teacherList);
                }
            }
        }

        for (Course course : courseMap.values()) {
            List<Teacher> teacherList = courseTeacherMap.get(course.getName());
            if (teacherList == null) {
                teacherList = new ArrayList<>();
            }
            course.setTeachers(teacherList);
        }

        List<CourseSchedule> courseScheduleList = new ArrayList<>();

        for (Map.Entry<String, Program> entry : programMap.entrySet()) {
            Program program = entry.getValue();
            List<Course> courses = program.getCourseList();
            List<Batch> batches = programBatchMap.get(program.getName());
            Map<String, Integer> courseIndices = programCourseIndices.get(program.getName());

            for (Course course : courses) {
                course = courseMap.get(course.getName());
                for (Batch batch : batches) {
                    program.setCourseList(new ArrayList<>(courseMap.values()));
                    program.setCourseIndices(courseIndices);

                    CourseSchedule courseSchedule = new CourseSchedule();
                    courseSchedule.setBatch(batch);
                    courseSchedule.setName(course.getName());

                    List<Teacher> availableTeachers = new ArrayList<>();
                    for (Teacher teacher : course.getTeachers()) {
                        if (teacher.getAvailableLocations().contains(batch.getLocation().getName())) {
                            availableTeachers.add(teacher);
                        }
                    }
                    if (availableTeachers.size() == 0) {
                        System.out.println("Error => " + batch.getName() + " : " + batch.getLocation().getName() + " : " + course.getName() + " -- has no faculty");
                        System.exit(1);
                    }

                    courseSchedule.setTeacherList(availableTeachers);

                    List<DateTimeSlots> dateTimeSlotsList = null;
//                    if ("PGPBABI Chennai Jan17".equals(batch.getName())) {
//                        dateTimeSlotsList = CourseDateTimeSlotsGenerator.generate(course, batch);
//                        dateTimeSlotsList = trimDateSlots(dateTimeSlotsList, course, batch, courses, courseMap);
//                    } else {
                        dateTimeSlotsList = DateTimeSlotsGenerator.generate(batch, course, courses, courseMap);
            //        }

                    courseSchedule.setDateTimeSlotsList(dateTimeSlotsList);
                    courseSchedule.setSlotsNum(course.getSlots().size());
                    courseScheduleList.add(courseSchedule);
                }
            }
        }
        glCalendar.setCourseScheduleList(courseScheduleList);
        long count = 0;
        for (CourseSchedule schedule : courseScheduleList) {
            if (schedule.getDateTimeSlotsList().size() == 0) {
                System.out.println(schedule.getBatch().getName() + " :: " + schedule.getName());
            }
            count += (schedule.getTeacherList().size() * schedule.getDateTimeSlotsList().size());
        }
        System.out.println(count);

        return glCalendar;
    }


    private static List<DateTimeSlots> trimDateSlots(List<DateTimeSlots> dateTimeSlotsList, Course course, Batch batch, List<Course> courses, Map<String, Course> courseMap) {
        LocalDate startDateBatch = batch.getStartDate();
        LocalDate yearTracker = LocalDate.of(startDateBatch.getYear(), startDateBatch.getMonth(), startDateBatch.getDayOfMonth());
        Map<Integer, LocalDate> startLocalDateIndices = new HashMap<>();
        yearTracker = yearTracker.minusMonths(1);
        List<Integer> monthlyResidencyDays = batch.getMonthlyResidencyDays();
        for (int i = 0; i < monthlyResidencyDays.size() + 3; i++) {
            yearTracker = yearTracker.plusMonths(1);
            LocalDate possibleStartDate = LocalDate.of(yearTracker.getYear(), yearTracker.getMonth(), 1);
            startLocalDateIndices.put(i, possibleStartDate);
        }

        int myCount = 0;
        for (Course course1 : courses) {
            Course course2 = courseMap.get(course1.getName());
            myCount += course2.getSlots().size();
            if (course2.getName().equals(course.getName())) {
                break;
            }
        }

        int divider = 5;
        if (batch.getName().startsWith("PGPM-Ex")) {
            divider = 4;
        } else if (batch.getName().equals("PGPBABI Chennai Jan17")) {
            divider = 10;
        }
        int idx = myCount / divider;
        if (batch.getName().equals("PGPBABI Chennai Jan17")) {
            idx *= 2; //Since chennai has got 5,0 residencies days, to handle missing residency days
        }

        LocalDate pLocalDate = startLocalDateIndices.get(idx);
        LocalDate ppLocalDate = pLocalDate.minusMonths(2);
        if (!batch.getName().equals("PGPBABI Chennai Jan17")) {
            ppLocalDate = pLocalDate.minusMonths(1);
        }
        ppLocalDate = LocalDate.of(ppLocalDate.getYear(), ppLocalDate.getMonth(), 1);
        LocalDate apLocalDate = pLocalDate.plusMonths(3);
        if (!batch.getName().equals("PGPBABI Chennai Jan17")) {
            apLocalDate = pLocalDate.plusMonths(1);
        }
        apLocalDate = LocalDate.of(apLocalDate.getYear(), apLocalDate.getMonth(), 1);

        int startIdx = 0;
        int endIdx = dateTimeSlotsList.size();
        for (int i = 0; i < dateTimeSlotsList.size(); i++) {
            DateTimeSlots dateTimeSlots = dateTimeSlotsList.get(i);
            List<DateTimeSlot> dateTimeSlotList = dateTimeSlots.getDateTimeSlots();

            if (startIdx == 0 && dateTimeSlotList.get(0).getDate().compareTo(ppLocalDate) > 0) {
                startIdx = i;
            }
            if (endIdx == dateTimeSlotsList.size() && dateTimeSlotList.get(0).getDate().compareTo(apLocalDate) > 0) {
                endIdx = i;
            }
        }
        return dateTimeSlotsList.subList(startIdx == 0 ? startIdx : startIdx, endIdx == dateTimeSlotsList.size() ? endIdx : endIdx + 1);
    }

    private static void displayCalendar(GLCalendar glCalendar) {
        Map<Batch, Map.Entry<LocalDate, Integer>> residencyCounterTracker = new HashMap<>();
        Map<DateTimeSlot, List<CourseSchedule>> calendar = Util.convertToCalendar(glCalendar);
        for (Map.Entry<DateTimeSlot, List<CourseSchedule>> entry : calendar.entrySet()) {
            for (CourseSchedule schedule : entry.getValue()) {
                System.out.print(entry.getKey().getDate());
                System.out.print("\t");
                System.out.print(entry.getKey().getTimeSlot());
                System.out.print("\t");
                System.out.print(schedule.getBatch().getName());
                System.out.print("\t");
                System.out.print(schedule.getName());
                System.out.print("\t");
                System.out.print(schedule.getTeacher().getName());
                System.out.print("\t");

                Map.Entry<LocalDate, Integer> localDateIntegerEntry = residencyCounterTracker.get(schedule.getBatch());
                if (localDateIntegerEntry == null) {
                    localDateIntegerEntry = new AbstractMap.SimpleEntry<>(entry.getKey().getDate(), 0);
                }
                LocalDate prevDate = localDateIntegerEntry.getKey();
                int preCount = localDateIntegerEntry.getValue();
                long daysBetween = ChronoUnit.DAYS.between(prevDate, entry.getKey().getDate());
                if (daysBetween > 1) {
                    preCount++;
                }
                localDateIntegerEntry = new AbstractMap.SimpleEntry<>(entry.getKey().getDate(), preCount);

                System.out.print("R-" + (preCount + 1));
                System.out.print("\t");
                System.out.print(schedule.getBatch().getLocation().getName());

                residencyCounterTracker.put(schedule.getBatch(), localDateIntegerEntry);

                System.out.println("");
            }
        }
    }

    private static void display(GLCalendar glCalendar) {
        int nullCounters = 0;
        List<CourseSchedule> courseScheduleList = glCalendar.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
//            System.out.println(courseSchedule.getName());
//            System.out.println(courseSchedule.getBatch().getName());
//            System.out.println(courseSchedule.getSlotsNum());
//            System.out.println(courseSchedule.getTeacher());
//            System.out.println(courseSchedule.getDateTimeSlots());
//
//            System.out.println("-----------------------------------");

            if (courseSchedule.getTeacher() == null) {
                nullCounters++;
            }
        }
        System.out.println(nullCounters);

    }

    private static GLCalendar generateUnplannedCalendar() {
        GLCalendar unplannedSolution = new GLCalendar();

        List<CourseSchedule> courseScheduleList = new ArrayList<>();
        List<Teacher> teacherList = new ArrayList<>();
        Teacher teacher1 = new Teacher();
        teacher1.setName("teacher_1");
        teacher1.setCanTeachCourses(new HashSet<>(Arrays.asList("course_1", "course_2")));
        //teacher1.setHolidays(Arrays.asList(LocalDate.parse("2016-01-02")));
        teacherList.add(teacher1);

        Teacher teacher2 = new Teacher();
        teacher2.setName("teacher_2");
        teacher2.setCanTeachCourses(new HashSet<>(Arrays.asList("course_1", "course_2")));
        teacher2.setAvailableLocations(new HashSet<>(Arrays.asList("bangalore")));
        teacherList.add(teacher2);

        Course course_1 = new Course();
        course_1.setName("course_1");
        course_1.setSlotsNum(2);
        course_1.setTeachers(teacherList);

        Course course_2 = new Course();
        course_2.setName("course_2");
        course_2.setSlotsNum(3);
        course_2.setTeachers(teacherList);

        Location location = new Location();
        location.setName("bangalore");
        location.setRooms(2);

        Batch batch1 = new Batch();
        batch1.setLocation(location);
        batch1.setName("batch_1");
        batch1.setStartDate(LocalDate.of(2016, 1, 1));
        batch1.setMinGapBetweenResidenciesInDays(30);
        batch1.setMaxGapBetweenResidenciesInDays(45);
        batch1.setMonthlyResidencyDays(Arrays.asList(2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3));
        batch1.setProgram(null);
        batch1.getPossibleResidencyDates();

        CourseSchedule courseSchedule_1 = new CourseSchedule();
        courseSchedule_1.setBatch(batch1);
        courseSchedule_1.setName("course_1");
        courseSchedule_1.setTeacherList(course_1.getTeachers());
        courseSchedule_1.setDateTimeSlotsList(CourseDateTimeSlotsGenerator.generate(course_1, batch1));

        CourseSchedule courseSchedule_2 = new CourseSchedule();
        courseSchedule_2.setBatch(batch1);
        courseSchedule_2.setName("course_2");
        courseSchedule_2.setTeacherList(course_2.getTeachers());
        courseSchedule_2.setDateTimeSlotsList(CourseDateTimeSlotsGenerator.generate(course_2, batch1));

        courseScheduleList.add(courseSchedule_1);
        courseScheduleList.add(courseSchedule_2);

        unplannedSolution.setCourseScheduleList(courseScheduleList);
        return unplannedSolution;
    }
}

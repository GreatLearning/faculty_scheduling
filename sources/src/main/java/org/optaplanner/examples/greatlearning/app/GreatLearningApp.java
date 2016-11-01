package org.optaplanner.examples.greatlearning.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.CourseDateTimeSlotsGenerator;
import org.optaplanner.examples.greatlearning.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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

        System.out.println(plannedSolution.getConstraintsBroken());

        display(plannedSolution);
    }

    private static GLCalendar loadFromFile() throws IOException {
        GLCalendar glCalendar = new GLCalendar();

        ObjectMapper objectMapper = new ObjectMapper();
        InputStream resourceAsStream = GreatLearningApp.class.getResourceAsStream("/org/optaplanner/examples/greatlearning/faculty_scheduling_input_Saba_v1.json");
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        JsonNode jsonNode = objectMapper.readTree(testData);
        JsonNode programs = jsonNode.get("programs");

        Iterator<Map.Entry<String, JsonNode>> programIterator = programs.fields();

        Map<String, Program> programMap = new HashMap<>();
        Map<String, Teacher> teacherMap = new HashMap<>();

        while (programIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = programIterator.next();
            Program program = new Program();
            String programName = entry.getKey();
            program.setName(programName);

            ArrayNode coursesNode = (ArrayNode) entry.getValue().get("courses");
            List<Course> courseList = new ArrayList<>();
            for (int i = 0; i < coursesNode.size(); i++) {
                String courseName = coursesNode.get(i).asText();
                Course course = new Course();
                course.setName(courseName);
                courseList.add(course);
            }
            program.setCourseList(courseList);

            ArrayNode residencyDaysCounts = (ArrayNode) entry.getValue().get("residency_days_count");
            List<Integer> monthlyResidencyDays = new ArrayList<>();
            for (int i = 0; i < residencyDaysCounts.size(); i++) {
                int count = residencyDaysCounts.get(i).asInt();
                monthlyResidencyDays.add(count);
            }
            program.setMonthlyResidencyDays(monthlyResidencyDays);

//            int minGapBetweenResidency = entry.getValue().get("min_gap_bw_residencies").asInt();
//            int maxGapBetweenResidency = entry.getValue().get("max_gap_bw_residencies").asInt();
//            int maxCourseInFlight = entry.getValue().get("max_courses_in_flight").asInt(); //TODO
//
//            program.setMaxGapBetweenResidenciesInDays(maxGapBetweenResidency);
//            program.setMinGapBetweenResidenciesInDays(minGapBetweenResidency);

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
                exceptionDates.add(LocalDate.parse(exceptionDays.get(i).asText()));
            }
            Location location = new Location();
            location.setName(locationName);
            location.setRooms(rooms);
            location.setHolidays(exceptionDates);

            locationMap.put(locationName, location);
        }

        ArrayNode batchesNode = (ArrayNode) jsonNode.get("batches");
        List<Batch> batchList = new ArrayList<>();
        for (int i = 0; i < batchesNode.size(); i++) {
            JsonNode batchNode = batchesNode.get(i);
            String batchName = batchNode.get("name").asText();
            String programName = batchNode.get("program").asText();
            String locationName = batchNode.get("location").asText();

            Location location = locationMap.get(locationName);

            LocalDate startDate = LocalDate.parse(batchNode.get("start_date").asText());
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
            batchList.add(batch);
        }

        JsonNode coursesNode = jsonNode.get("courses");
        Iterator<Map.Entry<String, JsonNode>> courseIterator = coursesNode.fields();
        List<Course> courseList = new ArrayList<>();
        while (courseIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = courseIterator.next();
            String courseName = entry.getKey();
            int duration = entry.getValue().get("duration").asInt(); //slotNum = duration/4
            Course course = new Course();
            course.setName(courseName);
            course.setSlotsNum(duration / 4);
            courseList.add(course);
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
                LocalDate date = LocalDate.parse(restrictedDays.get(i).asText());
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

        for (Course course : courseList) {
            List<Teacher> teacherList = courseTeacherMap.get(course.getName());
            if (teacherList == null) {
                teacherList = new ArrayList<>();
            }
            course.setTeachers(teacherList);
        }

        List<CourseSchedule> courseScheduleList = new ArrayList<>();
        for (Course course : courseList) {
            for (Batch batch : batchList) {
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

                courseSchedule.setDateTimeSlotsList(CourseDateTimeSlotsGenerator.generate(course, batch));
                courseSchedule.setSlotsNum(course.getSlotsNum());
                courseScheduleList.add(courseSchedule);
            }
        }

        glCalendar.setCourseScheduleList(courseScheduleList);
        return glCalendar;
    }

    private static void displayCalendar(GLCalendar glCalendar) {
        Map<DateTimeSlot, List<CourseSchedule>> calendar = Util.convertToCalendar(glCalendar);
        for (Map.Entry<DateTimeSlot, List<CourseSchedule>> entry : calendar.entrySet()) {
            for (CourseSchedule schedule : entry.getValue()) {
                System.out.print(entry.getKey());
                System.out.print("==>>");
                System.out.println(schedule.getBatch().getLocation().getName());
                System.out.print(schedule.getBatch().getName() + " ; " + schedule.getName() + " ; " + schedule.getTeacher());
                System.out.println("");
            }
            System.out.println("------------------------------------");
        }
    }

    private static void display(GLCalendar glCalendar) {
        int nullCounters = 0;
        List<CourseSchedule> courseScheduleList = glCalendar.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            System.out.println(courseSchedule.getName());
            System.out.println(courseSchedule.getBatch().getName());
            System.out.println(courseSchedule.getSlotsNum());
            System.out.println(courseSchedule.getTeacher());
            System.out.println(courseSchedule.getDateTimeSlots());

            System.out.println("-----------------------------------");

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

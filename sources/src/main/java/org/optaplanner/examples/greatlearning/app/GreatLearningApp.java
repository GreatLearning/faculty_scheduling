package org.optaplanner.examples.greatlearning.app;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.greatlearning.domain.*;
import org.optaplanner.examples.greatlearning.util.CourseDateTimeSlotsGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GreatLearningApp {
    public static void main(String[] args) {
        SolverFactory<GLCalendar> solverFactory = SolverFactory.createFromXmlResource(
                "org/optaplanner/examples/greatlearning/solver/glCalendarSolverConfig.xml");
        Solver<GLCalendar> solver = solverFactory.buildSolver();

        GLCalendar unplannedSolution = generateUnplannedCalendar();

        GLCalendar plannedSolution = solver.solve(unplannedSolution);

        display(plannedSolution);
    }

    private static void display(GLCalendar glCalendar) {
        List<CourseSchedule> courseScheduleList = glCalendar.getCourseScheduleList();
        for (CourseSchedule courseSchedule : courseScheduleList) {
            System.out.println(courseSchedule.getName());
            System.out.println(courseSchedule.getBatch().getName());
            System.out.println(courseSchedule.getTeacher().getName());
            System.out.println(courseSchedule.getDateTimeSlots());

            System.out.println("-----------------------------------");
        }

    }

    private static GLCalendar generateUnplannedCalendar() {
        GLCalendar unplannedSolution = new GLCalendar();

        List<CourseSchedule> courseScheduleList = new ArrayList<>();
        List<Teacher> teacherList = new ArrayList<>();
        Teacher teacher1 = new Teacher();
        teacher1.setName("teacher_1");
        teacher1.setCanTeachCourses(new HashSet<>(Arrays.asList("course_1", "course_2")));
        teacherList.add(teacher1);

        Teacher teacher2 = new Teacher();
        teacher2.setName("teacher_2");
        teacher2.setCanTeachCourses(new HashSet<>(Arrays.asList("course_1", "course_2")));
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

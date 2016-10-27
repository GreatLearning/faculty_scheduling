package org.optaplanner.examples.gl.app;

import com.google.common.collect.Sets;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.gl.domain.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vinodvr on 20/10/16.
 */
public class GLCalendarHelloWorldApp {

    public static void main(String[] args) {
        // Build the Solver
        SolverFactory<GLResidencyCalendar> solverFactory = SolverFactory.createFromXmlResource(
                "org/optaplanner/examples/gl/solver/glResidencyCalendarSolverConfig.xml");
        Solver<GLResidencyCalendar> solver = solverFactory.buildSolver();

        GLResidencyCalendar unsolvedGlResidencyCalendar = generateResidencyCalendar();

        // Solve the problem
        GLResidencyCalendar glResidencyCalendar = solver.solve(unsolvedGlResidencyCalendar);

        for (Residency residency : glResidencyCalendar.getResidencyList()) {
            System.out.println("Batch : " + residency.getBatch());
            System.out.println("Location : " + residency.getLocation());
            System.out.println("Dates : " + residency.getDates());
            System.out.println("Courses : " + residency.getCourses());
            System.out.println("Teachers : " + residency.getTeachers());
            System.out.println("---------------------------------");

        }

    }

    private static GLResidencyCalendar generateResidencyCalendar() {
        GLResidencyCalendar glResidencyCalendar = new GLResidencyCalendar();
        List<Residency> residencies = new ArrayList<>();

        Residency residency1 = new Residency();
        residency1.setBatch("Alpha");
        residency1.setLocation(new Location("room1", 2));
        residency1.setAllowedDates(Arrays.asList(new ResidencyDates(LocalDate.now(), LocalDate.now().plusDays(5)),
                new ResidencyDates(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15))));

        Course course = new Course("Course_1", Arrays.asList(new Teacher("Teacher_1", Sets.newHashSet("Course_1"))));
        residency1.setCourses(Arrays.asList(course));

        residencies.add(residency1);


        Residency residency2 = new Residency();
        residency2.setBatch("Alpha");
        residency2.setLocation(new Location("room1", 2));
        residency2.setAllowedDates(Arrays.asList(new ResidencyDates(LocalDate.now(), LocalDate.now().plusDays(5)),
                new ResidencyDates(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15))));

        Course course2 = new Course("Course_1", Arrays.asList(new Teacher("Teacher_1", Sets.newHashSet("Course_1"))));
        residency2.setCourses(Arrays.asList(course2));

        residencies.add(residency2);

        glResidencyCalendar.setResidencyList(residencies);
        return glResidencyCalendar;
    }
}

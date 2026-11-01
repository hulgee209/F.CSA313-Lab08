package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    static final Student ACTIVE_STUDENT =
            new Student("B001", "bat@must.edu.mn", true, List.of("CS101"));
    static final Student INACTIVE_STUDENT =
            new Student("B002", "dorj@must.edu.mn", false, List.of("CS101"));

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    /** Амжилттай бүртгэлийн замын stub-ууд: оюутан идэвхтэй, хичээл бий, өмнөх хичээл хангагдсан. */
    private void stubHappyPath() {
        when(repository.findStudent("B001")).thenReturn(ACTIVE_STUDENT);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));
    }

    // A1
    @Test
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        stubHappyPath();

        String result = service.register("B001", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("B001", "CS201");
        verify(emailSender).send(eq("bat@must.edu.mn"), anyString(), anyString());
    }

    // A2
    @Test
    void register_unknownStudent_returnsErrorNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("X999")).thenReturn(null);

        String result = service.register("X999", "CS201");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3
    @Test
    void register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmail() {
        when(repository.findStudent("B002")).thenReturn(INACTIVE_STUDENT);

        String result = service.register("B002", "CS201");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A4
    @Test
    void register_missingPrerequisite_returnsErrorPrerequisitesAndDoesNotSave() {
        when(repository.findStudent("B001")).thenReturn(ACTIVE_STUDENT);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101", "CS201"));

        String result = service.register("B001", "CS301");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A5
    @Test
    void register_emailSenderThrows_stillReturnsOkAndRegistrationIsSaved() {
        stubHappyPath();
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("B001", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("B001", "CS201");
        verify(emailSender).send(eq("bat@must.edu.mn"), anyString(), anyString());
    }

    // A6
    @Test
    void register_nullStudentId_throwsIllegalArgumentAndNoInteractions() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS201"));

        verifyNoInteractions(repository, emailSender);
    }

    // A7
    @Test
    void register_success_emailBodyContainsCourseCode() {
        stubHappyPath();
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register("B001", "CS201");

        verify(emailSender).send(eq("bat@must.edu.mn"), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("CS201"),
                "И-мэйлийн биед хичээлийн код байх ёстой: " + bodyCaptor.getValue());
        verify(emailSender).send(anyString(), anyString(), contains("CS201"));
    }

    // A8
    @ParameterizedTest(name = "[{index}] studentExists={0}, active={1}, prereqMet={2} -> {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_PREREQUISITES"
    })
    void register_invalidCase_returnsExpectedErrorAndDoesNotSave(
            boolean studentExists, boolean active, boolean prereqMet, String expected) {
        Student student = new Student("B001", "bat@must.edu.mn", active, List.of("CS101"));
        // Мөр бүр өөр алхам дээр зогсдог тул зарим stub ашиглагдахгүй — lenient() шаардлагатай.
        lenient().when(repository.findStudent("B001")).thenReturn(studentExists ? student : null);
        lenient().when(repository.courseExists("CS201")).thenReturn(true);
        lenient().when(repository.prerequisites("CS201"))
                .thenReturn(prereqMet ? List.of("CS101") : List.of("CS101", "CS150"));

        String result = service.register("B001", "CS201");

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}

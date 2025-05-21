import com.oocourse.library1.LibraryBookId;
import com.oocourse.library1.LibraryBookState;
import com.oocourse.library1.LibraryTrace;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Book {
    private final LibraryBookId id;
    private LibraryBookState state;
    private final List<LibraryTrace> trace;

    public Book(LibraryBookId id) {
        this.id = id;
        this.state = LibraryBookState.BOOKSHELF;
        this.trace = new ArrayList<>();
    }

    public List<LibraryTrace> getTrace() {
        return Collections.unmodifiableList(trace);
    }

    public void move(LocalDate date, LibraryBookState dest) {
        trace.add(new LibraryTrace(date, this.state, dest));
        this.state = dest;
    }
}

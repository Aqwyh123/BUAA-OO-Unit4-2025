import com.oocourse.library3.LibraryBookId;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryCloseCmd;
import com.oocourse.library3.LibraryCommand;
import com.oocourse.library3.LibraryOpenCmd;
import com.oocourse.library3.LibraryQcsCmd;
import com.oocourse.library3.LibraryReqCmd;
import com.oocourse.library3.LibraryReqCmd.Type;

import java.time.LocalDate;
import java.util.Map;

import static com.oocourse.library3.LibraryIO.PRINTER;
import static com.oocourse.library3.LibraryIO.SCANNER;

public class MainClass {
    public static void main(String[] args) {
        Map<LibraryBookIsbn, Integer> inventory = SCANNER.getInventory();
        Library library = new Library(inventory);
        while (true) {
            LibraryCommand command = SCANNER.nextCommand();
            if (command == null) {
                break;
            }
            execute(library, command);
        }
    }

    private static void execute(Library library, LibraryCommand command) {
        LocalDate date = command.getDate();
        if (command instanceof LibraryOpenCmd) {
            PRINTER.move(date, library.open(date));
        } else if (command instanceof LibraryCloseCmd) {
            PRINTER.move(date, library.close(date));
        } else if (command instanceof LibraryQcsCmd) {
            PRINTER.info(command, library.queryCredit(((LibraryQcsCmd) command).getStudentId()));
        } else if (command instanceof LibraryReqCmd) {
            LibraryReqCmd req = (LibraryReqCmd) command;
            Type type = req.getType();
            LibraryBookIsbn bookIsbn = req.getBookIsbn();
            LibraryBookId bookId;
            String userId = req.getStudentId();
            if (type == Type.QUERIED) {
                bookId = req.getBookId();
                PRINTER.info(date, bookId, library.queryTrace(bookId));
            } else if (type == Type.BORROWED) {
                LibraryBookId borrowedBookId = library.borrowBook(date, bookIsbn, userId);
                if (borrowedBookId != null) {
                    PRINTER.accept(command, borrowedBookId);
                } else {
                    PRINTER.reject(command);
                }
            } else if (type == Type.ORDERED) {
                boolean ordered = library.orderBook(date, bookIsbn, userId);
                if (ordered) {
                    PRINTER.accept(command);
                } else {
                    PRINTER.reject(command);
                }
            } else if (type == Type.RETURNED) {
                boolean isOverdue;
                bookId = req.getBookId();
                isOverdue = library.returnBook(date, bookId, userId);
                PRINTER.accept(command, isOverdue ? "overdue" : "not overdue");
            } else if (type == Type.PICKED) {
                LibraryBookId pickedBookId = library.pickBook(date, bookIsbn, userId);
                if (pickedBookId != null) {
                    PRINTER.accept(command, pickedBookId);
                } else {
                    PRINTER.reject(command);
                }
            } else if (type == Type.READ) {
                LibraryBookId readBookId = library.readBook(date, bookIsbn, userId);
                if (readBookId != null) {
                    PRINTER.accept(command, readBookId);
                } else {
                    PRINTER.reject(command);
                }
            } else if (type == Type.RESTORED) {
                bookId = req.getBookId();
                library.restoreBook(date, bookId, userId);
                PRINTER.accept(command);
            }
        }
    }
}

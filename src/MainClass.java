import com.oocourse.library2.LibraryBookId;
import com.oocourse.library2.LibraryBookIsbn;
import com.oocourse.library2.LibraryCloseCmd;
import com.oocourse.library2.LibraryCommand;
import com.oocourse.library2.LibraryOpenCmd;
import com.oocourse.library2.LibraryReqCmd;
import com.oocourse.library2.LibraryReqCmd.Type;

import java.time.LocalDate;
import java.util.Map;

import static com.oocourse.library2.LibraryIO.PRINTER;
import static com.oocourse.library2.LibraryIO.SCANNER;

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
                    PRINTER.accept(date, type, userId, borrowedBookId);
                } else {
                    PRINTER.reject(date, type, userId, bookIsbn);
                }
            } else if (type == Type.ORDERED) {
                boolean ordered = library.orderBook(bookIsbn, userId);
                if (ordered) {
                    PRINTER.accept(date, type, userId, bookIsbn);
                } else {
                    PRINTER.reject(date, type, userId, bookIsbn);
                }
            } else if (type == Type.RETURNED) {
                bookId = req.getBookId();
                library.returnBook(date, bookId, userId);
                PRINTER.accept(date, type, userId, bookId);
            } else if (type == Type.PICKED) {
                LibraryBookId pickedBookId = library.pickBook(date, bookIsbn, userId);
                if (pickedBookId != null) {
                    PRINTER.accept(date, type, userId, pickedBookId);
                } else {
                    PRINTER.reject(date, type, userId, bookIsbn);
                }
            } else if (type == Type.READ) {
                LibraryBookId readBookId = library.readBook(date, bookIsbn, userId);
                if (readBookId != null) {
                    PRINTER.accept(date, type, userId, readBookId);
                } else {
                    PRINTER.reject(date, type, userId, bookIsbn);
                }
            } else if (type == Type.RESTORED) {
                bookId = req.getBookId();
                library.restoreBook(date, bookId, userId);
                PRINTER.accept(date, type, userId, bookId);
            }
        }
    }
}

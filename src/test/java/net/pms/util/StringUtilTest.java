/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util;

import net.pms.TestHelper;
import static net.pms.util.StringUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringUtilTest {

	@BeforeEach
	public void setUp() {
		TestHelper.setLoggingOff();
	}

	@Test
	public void fillStringTest() {
		char[] chars = {'a', 'æ'};
		assertEquals(fillString(chars, 4), "aæaæaæaæ", "fillStringCharArray");
		assertEquals(fillString(' ', 10), "          ", "fillStringCharSpace");
		assertEquals(fillString(1333, 3), "\u0535\u0535\u0535", "fillStringCodePoint");
		assertEquals(fillString("\u0648\u0AA7\u184A", 2), "\u0648\u0AA7\u184A\u0648\u0AA7\u184A", "fillStringUnicodeString");
		assertEquals(fillString("", 100), "", "fillStringEmptyString");
		assertEquals(fillString("foo", 0), "", "FillStringZero");
	}

	@Test
	public void stripHTMLTest() {
		//(expected=IllegalArgumentException.class)
		assertEquals(stripHTML("<html><sometag></sometag><someothertag/><body>Sometext</body></html>"), "Sometext", "stripHTMLBasicBody");
		assertEquals(stripHTML("<html><sometag></sometag><someothertag/><body>Sometext <strong>someSTRONGtext</strong></body></html>"), "Sometext someSTRONGtext", "stripHTMLBodyWithTags");
		assertThrows(IllegalArgumentException.class, () -> {
			assertEquals(stripHTML("<html><header></header>Somecontent</html>"), "");
		}, "stripHTMLWithoutBody");
	}

	@Test
	public void getYearTest() {
		assertEquals(-1, getYear("1600"));
		assertEquals(1601, getYear("1601"));
		assertEquals(2099, getYear("2099"));
		assertEquals(-1, getYear("2100"));
		assertEquals(-1, getYear(""));
		assertEquals(-1, getYear("19834"));
		assertEquals(-1, getYear("91"));
		assertEquals(-1, getYear("089"));
		assertEquals(-1, getYear("foo1984"));
		assertEquals(-1, getYear("1984bar"));
		assertEquals(1984, getYear("foo 1984 bar 2003"));
		assertEquals(2011, getYear("2011-09-11"));
		assertEquals(2011, getYear("2011 September 9"));
		assertEquals(2011, getYear("2011-Sep-11"));
		assertEquals(2011, getYear("2011-Sep-11, Someday"));
		assertEquals(2011, getYear("2011. september 9."));
		assertEquals(2011, getYear("2011.9.11"));
		assertEquals(2011, getYear("2011.09.11"));
		assertEquals(2011, getYear("2011/09/11"));
		assertEquals(2011, getYear("Someday, September 11, 2011"));
		assertEquals(2011, getYear("September 11, 2011"));
		assertEquals(2011, getYear("Sep. 11, 2011"));
		assertEquals(2011, getYear("9/11/2011"));
		assertEquals(2011, getYear("9-11-2011"));
		assertEquals(2011, getYear("9.11.2011"));
		assertEquals(-1, getYear("09.11.11"));
		assertEquals(-1, getYear("09/11/11"));
		assertEquals(2011, getYear("2011 5 April"));
		assertEquals(2011, getYear("11/9-2011"));
		assertEquals(-1, getYear(null));
	}

	@Test
	public void isSameYearTest() {
		assertFalse(isSameYear(null, null));
		assertFalse(isSameYear("1600", null));
		assertFalse(isSameYear("1601", null));
		assertFalse(isSameYear(null, "2099"));
		assertFalse(isSameYear(null, "2100"));
		assertFalse(isSameYear("foo 1984 bar 2003", "2003"));
		assertFalse(isSameYear("1984", "1985"));
		assertTrue(isSameYear("9-11-2011", "2011 September 9"));
	}

	@Test
	public void isEqualTest() {
		// Null
		assertTrue(isEqual(null, null));
		assertFalse(isEqual(null, ""));
		assertTrue(isEqual(null, "", true));
		assertFalse(isEqual(null, "   "));
		assertTrue(isEqual(null, "   ", true));
		assertFalse(isEqual(null, " \t"));
		assertTrue(isEqual(null, " \t", true));
		assertFalse(isEqual(null, "foo"));
		assertFalse(isEqual(null, "bAR", false, false, false, null, false, 0));
		assertTrue(isEqual("     ", "\t\t    \t", true, true, null, 1, 5));

		// Blank
		assertFalse(isEqual("          ", " \t"));
		assertTrue(isEqual("          ", " \t", true));
		assertTrue(isEqual(" \t", " \t"));
		assertFalse(isEqual("", "bAR", false, false, false, null, false, 0));
		assertFalse(isEqual("       \t ", "bAR", false, false, false, null, false, 0));

		// Case
		assertFalse(isEqual("bar", "bAR", false, false, false, null, false, 0));
		assertTrue(isEqual("bar", "bAR", false, false, true, null, false, 0));
		assertTrue(isEqual("bar", "bar", false, false, false, null, false, 0));
		assertTrue(isEqual("bar", "bar", false, false, true, null, false, 0));
		assertTrue(isEqual("BAR", "BAR", false, false, false, null, false, 0));
		assertTrue(isEqual("BAR", "BAR", false, false, true, null, false, 0));

		// Trim
		assertFalse(isEqual(" bar", "bar", false, false, false, null, false, 0));
		assertTrue(isEqual(" bar", "bar", false, true, false, null, false, 0));
		assertTrue(isEqual(" bar", "bar\t", false, true, false, null, false, 0));
		assertTrue(isEqual("", "   \t", false, true, false, null, false, 0));

		// Shortest
		assertTrue(isEqual("foobar", "foobar", false, false, false, null, true, 0));
		assertTrue(isEqual("foobar", "foo", false, false, false, null, true, 0));
		assertTrue(isEqual("foo", "foobar", false, false, false, null, true, 0));
		assertTrue(isEqual("foo", "foobar", false, false, false, null, true, 3));
		assertFalse(isEqual("foo", "foobar", false, false, false, null, true, 4));

		// SubString
		assertTrue(isEqual("foo", "foobar", false, false, null, 0, 3));
		assertTrue(isEqual("foo", "foobar", false, false, null, 1, 3));
		assertTrue(isEqual("foo", "foobar", false, false, null, 2, 3));
		assertTrue(isEqual("foo", "bar", false, false, null, 2, 2));
		assertFalse(isEqualTo("foobar", "hoobar", false, false, null, 3));
		assertTrue(isEqualFrom("foobar", "hoobar", false, false, null, 3));

		// Combined
		assertTrue(isEqual("   ", "     ", true, false, false, null, true, 4));
		assertTrue(isEqual("   ", "     ", false, true, false, null, true, 4));
		assertTrue(isEqual("Foobar", " foobar     ", true, true, true, null, false, 4));
		assertTrue(isEqual("Foo", " foobar     ", true, true, true, null, true, 3));
		assertFalse(isEqual("Foo", " foobar     ", true, true, true, null, true, 4));
		assertTrue(isEqual("Foobar", "", true, false, true, null, true, 0));
		assertFalse(isEqual("Foobar", "", true, false, true, null, true, 1));
		assertFalse(isEqual("Foobar", "", true, false, true, null, false, 0));
		assertTrue(isEqual("Foobar", "fOO", true, false, true, null, true, 0));
		assertTrue(isEqual("FooBar", "foobar", true, true, null, 3, -1));
		assertTrue(isEqual("FooBar", "foobar", true, true, null, 2, 5));
		assertTrue(isEqual("FooBar", "foobar", false, true, null, 2, 5));
		assertFalse(isEqual("FooBar", "foobar", true, false, null, 2, 5));
		assertTrue(isEqual("FooBar", "foobar", false, true, null, -1, 5));
	}

}

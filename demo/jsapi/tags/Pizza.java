/* Copyright 1999,2004 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package demo.jsapi.tags;

import java.util.Vector;

public class Pizza implements OrderItem {
    public Vector toppings = new Vector();

    public Pizza() {
    }

    public void addTopping(String topping) {
        toppings.add(topping);
    }

    public String toString() {
        int numToppings = toppings.size();
        if (numToppings == 0) {
            return "plain pizza.";
        } else {
            StringBuffer sb = new StringBuffer("pizza with ");
            for (int i = 0; i < numToppings; i++) {
                sb.append(toppings.elementAt(i));
                if (numToppings > 1) {
                    if (i == (numToppings - 2)) {
                        sb.append(" and ");
                    } else if (i < (numToppings - 1)) {
                        sb.append(", ");
                    }
                }
            }
            sb.append(".");
            return sb.toString();
        }
    }
}


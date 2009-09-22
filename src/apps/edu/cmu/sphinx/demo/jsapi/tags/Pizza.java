/* Copyright 1999,2004 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.demo.jsapi.tags;

import java.util.List;
import java.util.ArrayList;

public class Pizza implements OrderItem {
    public List<String> toppings = new ArrayList<String>();

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
            StringBuilder sb = new StringBuilder("pizza with ");
            for (int i = 0; i < numToppings; i++) {
                sb.append(toppings.get(i));
                if (numToppings > 1) {
                    if (i == (numToppings - 2)) {
                        sb.append(" and ");
                    } else if (i < (numToppings - 1)) {
                        sb.append(", ");
                    }
                }
            }
            sb.append('.');
            return sb.toString();
        }
    }
}


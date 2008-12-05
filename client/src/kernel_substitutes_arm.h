/* Copyright (c) 2007, 2008  Barcelona Supercomputing Center -
   Centro Nacional de Supercomputacion 
   This file is part of XtreemFS.

   XtreemFS is part of XtreemOS, a Linux-based Grid Operating
   System, see <http://www.xtreemos.eu> for more details. The
   XtreemOS project has been developed with the financial support
   of the European Commission's IST program under contract
   #FP6-033576.

   XtreemFS is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   XtreemFS is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of 
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS.  If not, see <http://www.gnu.org/licenses/>.
 */



#ifndef KERNEL_SUBSTITUTES_ARM_H
#define KERNEL_SUBSTITUTES_ARM_H

//
// WARNING: This stuff is architecture dependent!!!
//

#ifdef CONFIG_SMP
#error SMP is NOT supported
#endif

/*
 * Save the current interrupt enable state & disable IRQs
 */
#define local_irq_save(x)                               \
        do {                                            \
          unsigned long temp;                           \
          __asm__ __volatile__(                         \
"       mov     %0, pc          @ save_flags_cli\n"     \
"       orr     %1, %0, #0x08000000\n"                  \
"       and     %0, %0, #0x0c000000\n"                  \
"       teqp    %1, #0\n"                               \
          : "=r" (x), "=r" (temp)                       \
          :                                             \
          : "memory");                                  \
        } while (0)


/*
 * restore saved IRQ & FIQ state
 */
#define local_irq_restore(x)                            \
        do {                                            \
          unsigned long temp;                           \
          __asm__ __volatile__(                         \
"       mov     %0, pc          @ restore_flags\n"      \
"       bic     %0, %0, #0x0c000000\n"                  \
"       orr     %0, %0, %1\n"                           \
"       teqp    %0, #0\n"                               \
          : "=&r" (temp)                                \
          : "r" (x)                                     \
          : "memory");                                  \
        } while (0)




typedef struct { int counter; } atomic_t;


#define ATOMIC_INIT(i)  { (i) }

/**
 * atomic_read - read atomic variable
 * @v: pointer of type atomic_t
 * 
 * Atomically reads the value of @v.
 */ 
#define atomic_read(v)          ((v)->counter)

/**
 * atomic_set - set atomic variable
 * @v: pointer of type atomic_t
 * @i: required value
 * 
 * Atomically sets the value of @v to @i.
 */ 
#define atomic_set(v,i)         (((v)->counter) = (i))

static __inline__ int atomic_add_return(int i, atomic_t *v)
{
        unsigned long flags;
        int val;

        local_irq_save(flags);
        val = v->counter;
        v->counter = val += i;
        local_irq_restore(flags);

        return val;
}

static __inline__ int atomic_sub_return(int i, atomic_t *v)
{
	return atomic_add_return(-i,v);
}

#define atomic_inc(v)           (void) atomic_add_return(1, v)
#define atomic_dec(v)           (void) atomic_sub_return(1, v)
#define atomic_inc_return(v)    (atomic_add_return(1,v))
#define atomic_dec_return(v)    (atomic_sub_return(1,v))


#endif

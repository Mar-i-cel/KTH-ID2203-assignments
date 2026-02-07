// ### Exercise 1: Build an Eventually Perfect Failure Detector
//
// An Eventually Perfect Failure Detector (EPFD), in Kompics terms, is a component that **provides** the following port _(already imported in the code)_.
//
//```scala
//  class EventuallyPerfectFailureDetector extends Port {
//    indication[Suspect];
//    indication[Restore];
//  }
//```
//
// Simply put, your component should indicate or ‘deliver’ to the application the following messages:
//
//```scala
//  case class Suspect(src: Address) extends KompicsEvent;
//  case class Restore(src: Address) extends KompicsEvent;
//```
//
// As you have already learnt from the course lectures, an EPFD, defined in a partially synchronous model, should satisfy the following properties:
//
//  1.  **Completeness**: Every process that crashes should be eventually suspected permanently by every correct process
//  2.  **Eventual Strong Accuracy**: No correct process should be eventually suspected by any other correct process
//
// To complete this assignment you will have to fill in the missing functionality denoted by the commented sections below and pass the property checking test at the end of this notebook.  
// The recommended algorithm to use in this assignment is _EPFD with Increasing Timeout and Sequence Numbers_, which you can find at the second page of this [document](https://courses.edx.org/asset-v1:KTHx+ID2203.1x+2016T3+type@asset+block@epfd.pdf) in the respective lecture.

package se.kth.edx.id2203.templates

import se.kth.edx.id2203.core.Ports._
import se.kth.edx.id2203.templates.EPFD._
import se.kth.edx.id2203.validation._
import se.sics.kompics.network._
import se.sics.kompics.sl.{ Init, _ }
import se.sics.kompics.timer.{ ScheduleTimeout, Timeout, Timer }
import se.sics.kompics.{ KompicsEvent, Start, ComponentDefinition => _, Port => _ }

import scala.collection.mutable._

//Define initialization event
object EPFD {

  //Declare custom message types related to internal component implementation
  case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout); //are this the time units where each process after time units sends a heartbeat?

  case class HeartbeatReply(seq: Int) extends KompicsEvent;//A KompicsEvent used as a response to heartbeat requests, so this one is for when i recieve a heartbeat from another process and i have to respond saying im alive?
  //When your process receives a HeartbeatRequest, you send back a HeartbeatReply with the same sequence number. This is how you say "Yes, I'm alive!" to the process that asked.
  
  case class HeartbeatRequest(seq: Int) extends KompicsEvent;//a heartbeat to ask if a process is alive, Sent periodically to all processes being monitored, so this is for asking other process if they r alive?
  //This is the message you send to other processes to check if they're alive. You send this periodically (based on the timer) to every process you're monitoring. The seq (sequence number) helps track which round of checking this is, so old replies don't confuse the algorithm.

}

//Define EPFD Implementation
class EPFD(epfdInit: Init[EPFD]) extends ComponentDefinition {

  //EPFD subscriptions
  val timer = requires[Timer];//This is the timer service that tells EPFD when to do its periodic checks. You use it to:
    //Schedule: "Hey timer, wake me up in period milliseconds"
    //Get notified: Timer sends back CheckTimeout event when time is up
    //Then EPFD sends heartbeat requests to all processes

  val pLink = requires[PerfectLink];//This is the network channel for:
    //Sending HeartbeatRequest messages to other processes
    //Receiving HeartbeatReply messages from other processes

  val epfd = provides[EventuallyPerfectFailureDetector];//epfd (Provides port) → EXTERNAL REPORTING
    //Suspect(node) announcements to applications
    //Restore(node) announcements to applications
    //EPFD-to-APPLICATION talk (reporting results)

  // EPDF component state and initialization
  val self = epfdInit match {
    case Init(s: Address) => s
  }//the  process's own address. So it knows who am i

  val topology = cfg.getValue[List[Address]]("epfd.simulation.topology");// This is the list of all processes in the system that EPFD should monitor (including itself).
  val delta = cfg.getValue[Long]("epfd.simulation.delay");//This is the base timeout increment. It's a constant that determines how much to increase the timeout period when needed.

  var period = cfg.getValue[Long]("epfd.simulation.delay");//It IS "time to wait before checking everyone again"
  //When it increments: When EPFD detects that alive and suspected sets overlap (some suspected processes are actually alive), it increases period by delta

  var alive = Set(cfg.getValue[List[Address]]("epfd.simulation.topology"): _*);//this is the set of processes that we know are alive i guess
  var suspected = Set[Address]();//this is the set of process we know are suspected of being dead i guess
  var seqnum = 0; //This is a sequence number/counter that:
    //Increments each checking round
    //Attached to HeartbeatRequest messages
    //Round 1: Send HeartbeatRequest(seq=1) → Expect HeartbeatReply(seq=1)

  def startTimer(delay: Long): Unit = {
    val scheduledTimeout = new ScheduleTimeout(period);
    scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout));
    trigger(scheduledTimeout -> timer);
  }

  //EPFD event handlers
  ctrl uponEvent {
    case _: Start => {
      /* WRITE YOUR CODE HERE */
      startTimer(period)
    }
  }


//EPFD only suspects failed processes, strong completness
//EPDF eventually no correct process is suspected by any correct process
//every Y time units and heartbeat to all process
//each process waits T time unites
//  IF did NOT get a heartbeat from P
//    indicate <suspect, P> If P is not in the suspect set
//    Put P in suspect set
//  IF did get heartbeat from P AND P is in suspect set
//    indicate <restore, P> and remove P from suspect set
//    increse timeout
  timer uponEvent {// so this is where the heartbeats are
    case CheckTimeout(_) => {
      if (!alive.intersect(suspected).isEmpty) {
        /* WRITE YOUR CODE HERE */
        period = period + delta;
      }

      seqnum = seqnum + 1;

      for (p <- topology) {
        if (!alive.contains(p) && !suspected.contains(p)) {
         /* WRITE YOUR CODE HERE */
         suspected = suspected + p;
         trigger(Suspect(p) -> epfd);

        } else if (alive.contains(p) && suspected.contains(p)) {
          suspected = suspected - p;
          trigger(Restore(p) -> epfd);
        }
        trigger(PL_Send(p, HeartbeatRequest(seqnum)) -> pLink);
      }

      alive = Set[Address]();
      startTimer(period);
    }
  }


  pLink uponEvent {
    case PL_Deliver(src, HeartbeatRequest(seq)) => {
         /* WRITE YOUR CODE HERE */
         trigger(PL_Send(src, HeartbeatReply(seq)) -> pLink);
    }
    case PL_Deliver(src, HeartbeatReply(seq)) => {
        //alive = alive + src
         /* WRITE YOUR CODE HERE */
         if (seq == seqnum) {
            alive = alive + src
        }
    }
  }
};

object EPFDTemplate extends App {
  checkEPFD[EPFD]();
}

#ifndef cb_time_clock_hpp
#define cb_time_clock_hpp

#include <sys/time.h>
#include <cmath>
#include <string>
#include <sstream>
#include <iomanip>
#include <fstream>
#include <vector>
#include "cycle.h"

namespace cb
{
namespace time
{

/**
 * Abstract clock interface
 */
class Clock
{
public:
	typedef double TimeT;
	virtual ~Clock() {};

	virtual void start() = 0;
	virtual TimeT elapsed() const = 0;
};


class CycleClock : public Clock
{
public:
	CycleClock()
	{
		start();
	}

	virtual void start()
	{
		startTime = getticks();
	}

	virtual TimeT elapsed() const
	{
		return (TimeT)(getticks() - startTime);
	}

private:
	ticks startTime;	
};

#ifdef CB_NANO_WALLCLOCK

//http://blog.stalkr.net/2010/03/nanosecond-time-measurement-with.html
class WallClock : public Clock
{
public:
	WallClock()
	{
		start();
	}

	virtual void start()
	{
		clock_gettime(CLOCK_REALTIME, &startTime);
	}

	virtual TimeT elapsed() const
	{
		struct timespec stopTime;
		clock_gettime(CLOCK_REALTIME, &stopTime);
		return (TimeT)((stopTime.tv_sec - startTime.tv_sec) * 1000000000 +  (stopTime.tv_nsec - startTime.tv_nsec));
	}

private:
	struct timespec startTime;
};

#else

class WallClock : public Clock
{
public:
	WallClock()
	{
		start();
	}

	virtual void start()
	{
		gettimeofday(&startTime, 0);
	}

	virtual TimeT elapsed() const
	{
		struct timeval stopTime;
		gettimeofday(&stopTime, 0);
		return (TimeT)((stopTime.tv_sec - startTime.tv_sec) * 1000000 +  (stopTime.tv_usec - startTime.tv_usec));
	}

private:
	struct timeval startTime;	
};

#endif

class MetaClock : public Clock
{
public:
	MetaClock(bool wall = false)
		: clock(wall ? (Clock*)new cb::time::WallClock() : (Clock*)new cb::time::CycleClock())
	{
		clock->start();
	}

	virtual ~MetaClock()
	{
		delete clock;
	}

	virtual void start()
	{
		clock->start();
	}

	virtual TimeT elapsed() const
	{
		return clock->elapsed();
	}

private:
	Clock* clock;
};

class TimeAverageVariance
{
public:
	typedef Clock::TimeT TimeT;
	
	/** \brief initialize with zero */
	TimeAverageVariance() : M(0), S(0), k(0){}

	/** \brief specify number of times to initialise vector with the right size */
	TimeAverageVariance(size_t n) : M(0), S(0), k(0) { times.reserve(n); }
	
	/** \brief uses the \c elapsed method */
	void add(Clock const& c) { add(c.elapsed()); }

	/** \brief add the time value */
	void add(TimeT val)
	{
		times.push_back(val);
		++k;
		TimeT delta = val - M;
		M = M + delta/(TimeT)k;
		S = S + delta*(val-M);

		if (k == 1)
		{
			min_ = val;
			max_ = val;
		}
		else
		{
			min_ = val < min_ ? val : min_;
			max_ = val > max_ ? val : max_;
		}

	}

	/** \brief number of times a value was added. */
	size_t count() const { return k; }
		
	/** \brief average of all added values. */
	TimeT average() const { return M; }

	/** \brief median of all added values. */
	TimeT median() const
	{
		// NOTE: when comparing this with the mathematical definition, keep in mind our indices start with 0
		const size_t n = times.size();
		if(n == 0)
		{
			return 0;
		}
		else if((n % 2) == 0) // even number of vaules
		{
			return 0.5 * (times[(n / 2) - 1] + times[n / 2]);
		}
		else // uneven number of values
		{
			return times[n / 2];
		}
	}

	/** \brief minimum of all added values. */
	TimeT min() const { return min_; }

	/** \brief maximum of all added values. */
	TimeT max() const { return max_; }


	/** \brief variance of all added values. */
	TimeT variance() const 
	{ 
		return (k<=1) ? (TimeT)0 : S/(TimeT)(k-1); 
	}

	/** \brief size of the confidence interval.
	 *
	 * The confidence interval would be [average - error, average + error].
	 *
	 * Doesn't do student's-t test, but simply uses the normal distribution.
	 * But it should be good enough to repeat experiments until the
	 * intervall is small enough.
	 *
	 * @returns the delta value for the 95% confidence interval.
	 */
	TimeT error() const
	{ 
		return 1.96*std::sqrt(variance()/count());
	}

	TimeT stderror() const
	{
		return std::sqrt(variance()/count());
	}

	/** \brief Returns relative error, so you can repeat
	 * measurements until the error is relatively small.
	 */
	TimeT relerror() const {
		return error()/average();
	}

	static std::string getHeaderString()
	{
		std::stringstream ss;
		ss << "average" << "\t"
		   << "median" << "\t"
		   << "min" << "\t"
		   << "max" << "\t"
		   << "error" << "\t"
		   << "rel_error" << "\t"
		   << "std_error" << "\t"
		   << "variance" << "\t"
		   << "count";
		return ss.str();
	}

	/** \brief Returns all data in a string, separated by tabs
	 * (average | error | relative error | variance | number)
	 */
	std::string toString() const
	{
		std::stringstream ss;
		ss << std::scientific // << std::fixed
		   << this->average() << "\t"
		   << this->median() << "\t"
		   << this->min() << "\t"
		   << this->max()  << "\t"
		   << this->error() << "\t"
		   << this->relerror() << "\t"
		   << this->stderror() << "\t"
		   << this->variance() << "\t"
		   << this->count();
		return ss.str();
	}

	void toFile(std::string filename) const
	{
		std::ofstream file(filename.c_str());
		file << std::scientific;

		for(size_t i = 0; i < times.size(); ++i)
		{
			file << times[i] << std::endl;
		}

		file.close();
	}

	TimeAverageVariance operator+(const TimeAverageVariance& other) const
	{
		TimeAverageVariance result;

		if(this->times.size() != other.times.size())
		{
			std::cout << "TimeAverageVariance::operator+: Warning, operand sizes do not match!" << std::endl;
			return result;
		}

		for(size_t i = 0; i < this->times.size(); ++i)
			result.add(this->times[i] + other.times[i]);
		return result;
	}

	TimeT sum() const
	{
		TimeT result = 0;
		for(size_t i = 0; i < times.size(); ++i)
			result += times[i];
		return result;
	}

private:
	TimeT M; //< mean or average
	TimeT S; //< variance
	size_t k; //< event counter
	std::vector<TimeT> times;
	TimeT min_; //< global maximum
	TimeT max_; // < global minimum
};

}; // namespace time

}; // namespace cb

#endif

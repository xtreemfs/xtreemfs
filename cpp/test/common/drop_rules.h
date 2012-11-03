/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_DROP_RULES_H_
#define CPP_TEST_COMMON_DROP_RULES_H_

namespace xtreemfs {
namespace rpc {

/** This interface is used for arbitrary implementations of drop rules
  * for RPC requests processed by TestRPCServer.
  */
class DropRule {
 public:
  /** This method is called for every received process */
  virtual bool DropRequest(uint32_t proc_id) = 0;

  /** This method is used to determine whether or not a rule can be deleted */
  virtual bool IsPointless() const = 0;

  /** Helper predicate function for std::algorithm */
  static bool IsPointlessPred(const DropRule* rule) {
    return rule->IsPointless();
  }

  virtual ~DropRule() {};
};

/** This rule skips m requests before dropping n requests. Afterwards, the rule
 *  becomes pointless. The proc_id is not checked.
 */
class SkipMDropNRule : public DropRule {
 public:
  SkipMDropNRule(size_t skip, size_t drop) : skip_count_(skip), drop_count_(drop) {}

  virtual bool DropRequest(uint32_t proc_id) {
    if (skip_count_ == 0 && drop_count_ > 0) {
      --drop_count_;
      return true;
    } else if (skip_count_ > 0) {
      --skip_count_;
    }
    return false;
    // or just:
    // return skip_count_-- > 0 ? ++skip_count_ : drop_count_-- > 0 ? true : ++drop_count_;
  }

  virtual bool IsPointless() const {
    return (drop_count_ == 0) && (skip_count_ == 0);
  }

 private:
  size_t skip_count_;
  size_t drop_count_;
};

/** This rule is used in combination with another rule which will be only
 *  called when the proc_id passed to DropRequest matches the one passed
 *  to the ctor.
 */
class ProcIDFilterRule : public DropRule {
 public:
  /** Construct a proc_id filtered rule from an existing rule.
   *  The ownership of rule's pointee is transfered to the created instance.
   */
  ProcIDFilterRule(uint32_t proc_id, DropRule* rule)
      : proc_id_(proc_id), rule_(rule) {}

  virtual bool DropRequest(uint32_t proc_id) {
    return proc_id == proc_id_ ? rule_->DropRequest(proc_id) : false;
  }

  virtual bool IsPointless() const {
    return rule_->IsPointless();
  }

 private:
  uint32_t proc_id_;
  boost::scoped_ptr<DropRule> rule_;
};

/** This rule drops n requests without checking the proc_id. */
class DropNRule : public DropRule {
public:
  DropNRule(size_t count) : count_(count) {}

  virtual bool DropRequest(uint32_t proc_id) {
    if (count_ > 0) {
      --count_;
      return true;
    } else {
      return false;
    }
  }

  virtual bool IsPointless() const {
    return count_ == 0;
  }

 private:
  size_t count_;
};

/** This rule drops all rules with a certain proc_id. This rule will never
 *  become pointless.
 */
class DropByProcIDRule : public DropRule {
public:
  DropByProcIDRule(uint32_t proc_id)
      : proc_id_(proc_id) {}

  virtual bool DropRequest(uint32_t proc_id) {
    return proc_id_ == proc_id;
  }

  virtual bool IsPointless() const {
    return false;
  }

private:
  size_t proc_id_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif

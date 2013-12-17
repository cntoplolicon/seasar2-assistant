Seasar2 Assistant
=================

Seasar2 Assistant is an Eclipse plugin to boost productivity for legacy seasar2 users.

### Installation ###

Download the archive file. Open Help->Install New Software... and add the archive as update site.

### Configuration ###

Seasar2 Assistant is attempt to use on standard project created by [Doteng](http://dolteng.sandbox.seasar.org/),
but not required. In project property page, turn on **use Seasar2 Assistant**. For project with standard layout,
the view root and default root package should have been correctly set. Specify these two values for non-standard
ones. See below about other options.

### Checking Scope Strings ###

Turn on **check scope declerations of properties in page class** on property page and select the error level. **warning** is recommended. Seasar2 Assistant will check the
existence of properties declared in `PAGE_SCOPE`, `REDIRECT_SCOPE` and `SUBAPPLICATION_SCOPE` strings when editing java
source.

### Generate S2Hibernate Dao ###

When the active editor is showing the entity class, you can press Ctrl+Shift+1 to switch to corresponding
S2Hibernate Dao. If the dao doesn't exist, an new interface wizard will be prompted. Turn on **generate common
methods when creating s2hibernate dao** to generate commons methods as `save()` or `delete()`

### EL Expression for Page Class ###

When editing Teeda HTML, press Ctrl+Shift+2 to insert a fragment of EL expression representing the page class. 
For example, for *{viewRoot}/sub1/sub2/test.html*, `sub1_sub2_testPage.` will be inserted.

### Generate ARGS and HQL for S2Hibernate Dao ###

Place the cursor at the method of S2Hibernate Dao and press Ctrl+Shift+3 to generate a `MethodName_ARGS` string.
Press Ctrl+Shift+3 again to generate a stub of `MethodName_HQL`.


# slothcfg

*&#34;Because I just... don't feel like managing two config files... &#42;sigh&#42;...&#34;*
<!-- 
Regex to create anchors for headings:
 Search: ^([#]{2,}+) (\S+)(.*)
Replace: $1 $2$3<a name="$2"/>

Regex for TOC links IN SELECTION:
 Search: ^(\s*)- (\S+)(.*)
Replace: $1- [$2$3](#$2)
-->
- [Features](#Features)
- [Installation](#Installation)
- [Usage](#Usage)
  - [Sticky profiles](#Sticky)
  - [Project map access](#Project)
- [Configuration](#Configuration)
- [Forked version of configleaf](#Forked)
  - [Upgrading from configleaf](#Upgrading)
- [News](#News)
- [License](#License)

## Features<a name="Features"/>

slothcfg for Leiningen 2 fills in the missing features in Leiningen 2's profile support:

+ Makes the `project.clj` file available to your code at runtime so that you can use
  it as a config file!
+ Persistent ("sticky") profiles, which can be set and remain in effect until unset.
+ Built-in templates to create .cljs, .cljx or .cljs config files.

## Installation<a name="Installation"/>

To install slothcfg, add the following to your project map as a plugin:

```
[slothcfg "1.0.0"]
```

That is all you need. But you will probably also want to add two
directories to your `.gitignore` file. The first is the directory
`.slothcfg`, which will be in the same directory as your
project.clj. This directory holds the currently active profile. The
second is the namespace that is automatically generated by slothcfg
with your profile values. In most of the examples above, you'd want to
add "src/cfg/current.clj" or possibly "src/cfg" to your `.gitignore`, if there are no other files you will have in `src/cfg` that you wish to check into git.

## Usage<a name="Usage"/>

The first step is learning about
[Leiningen's profiles](https://github.com/technomancy/leiningen/blob/stable/doc/PROFILES.md), in
the "Profiles" section. slothcfg only provides user interface to
turn profiles on and off, and to make them available to the code in
the project itself, so you need to understand how they work.

### Sticky profiles<a name="Sticky"/>

The only user-operable way of controlling profiles in use is using
Leiningen's built-in `with-profile` task, which requires you to list
all the profiles you'd like to have in effect for the task that is
given as its argument.

When slothcfg is installed, you can get a list of the currently
active profiles by doing `lein profiles`:

```
[prompt]$ lein profiles
debug
default
offline
test
user

Current sticky profiles:
```

Here, slothcfg has modified the built in `profiles` task to also
print out the currently active sticky profiles. In this case, since
none have been set, there are no current sticky profiles. We can set a
few profiles using the `set-profile` task:

```
[prompt]$ lein set-profile test stage
Warning: Unknown profile :stage is being set active.
Current sticky profiles: #{:test :stage}
```

Now two profiles have been set to active, `:test` and
`:stage`. slothcfg has warned that it cannot find any profile called
`:stage`, as it isn't present in the project map. But it has still set
the profile to stick.

If we set `:stage` by mistake, we can unset that profile using the `unset-profile` command:

```
[prompt]$ lein unset-profile stage
Current sticky profiles: #{:test}
```

If we wish to remove all sticky profiles at once, we can simply call
`unset-profile` with the `--all` flag:

```
[prompt]$ lein unset-profile --all
All profiles unset.
[prompt]$ lein profiles
debug
default
offline
test
user

Current sticky profiles:
```

Note that none of this is modifying the project map. It is simply
stashing a bit of state that will be automatically applied when you
run Leiningen tasks. This state is stored in the file
`.slothcfg/current` in your project root. This file can be deleted
at any time with no ill effect (other than unsticking all the profiles
that were set.

Finally, note that the built-in `with-profile` task still works the
same as it ever did. Any profiles it sets are added to the set of
sticky profiles already in effect, as sticky profiles are as if there
is an implicit `with-profile` with those tasks.

### Project map access<a name="Project"/>

The second half of slothcfg is making the project map, with all
active profiles merged in, available to the project itself, in
addition to within leiningen's own process. It does this in part by
outputting a Clojure source file into a location of your choice (by
default `{first of :source-paths project key}/cfg/current.clj`) that
contains the project map. Any code that is interested in the project
map, whether it is running in Leiningen, in your project, or in a JAR
built by Leiningen with slothcfg installed, can access it by loading
that namespace and accessing the `project` variable in it.

As an example, suppose you run the `repl` task:

```
[prompt]$ lein repl
Connection opened on localhost port 4005.
```

Then within the repl, you can do the following:

```
user> (use 'cfg.current)
nil
user> (prn project)
{:compile-path "/path/to/my-great-project/target/classes", :group "slothcfg", ...}
nil
```

So we can see that we were able to use the `cfg.current` namespace and
find the entire project map in the `project` var from that
namespace. As you would expect, you can now write code that `require`s
the `cfg.current` namespace and changes its behavior based on its
contents. This code will continue to work even when a JAR is built
from this project. The project map available in the JAR will be based
on the configurations that were included when the JAR was built
(either through sticky profiles or the `with-profile` task or none,
depending on which is the case).

## Configuration<a name="Configuration"/>

You can change the behavior of slothcfg by setting some values in the
project map. slothcfg's configuration goes in the `:slothcfg` key, which
should contain a map of option names to their values. Here are the current
keys:

- `:config-source-path` - Set this key to the path to the source directory to
  output the project map namespace into. Note that this is the location of the
  source directory, not the full path of the file the namespae is in; that
  will be named automatically based on the namespace's name.
  - Default: the first entry in the `:source-paths` key in the project.
- `:file-ext` - String with the file extension for the saved file. Useful for
  integration with [cljx](https://github.com/lynaghk/cljx).
  - Default: ".clj" if not specified
- `:namespace` - Set this key to the name of the namespace, as a symbol, that
  you want the project map to be output to.
  - Default: `cfg.current` if not specified
- `:var` - Set this key to the name of the var, as a symbol, that you want the
  project map to be output to.
  - Default: `project`
- `:verbose` - Set this key to true if you'd like slothcfg to print out
  which profiles are included whenever a task is run.
  - Default: `false`
- `:keyseq` - A keyseq to the subset of the project map you want to output.
  For example, set to `[:config]` to output only everything under `:config`.
  - Default: [] ; empty. the entire project map is output.
- `:middleware` - A function that takes a project map and outputs a transformed
  version of it. Note that if `:keyseq` is specified, it operates on that subset.
  - Default: No filtering is done. Equivalent to: `(fn [p] p)`
- `:template` - Optional string specifying a custom template on the classpath
  for use with [stencil](https://github.com/davidsantiago/stencil). Overrides
  the default templates that slothcfg uses based on the value of :file-ext
  (currently: .clj, .cljx, .cljs).
- `:never-sticky` - If specified, this should be a vector containing a list of
  profile name keys that should never be set sticky. For example, you can put
  your production profile in this key to make sure you don't accidentally set
  yourself into a production profile.

So for example, if the project map has the following map in the `:slothcfg`
key:

```clojure
:slothcfg {:config-source-path "src/main/clojure"
             :namespace myproject.config
             :var config
             :keyseq [:config]
             :verbose true}
````

Then the project map will be at `myproject.config/project`, which is
in the file `src/main/clojure/myproject/config.clj`. When you run the
command:

```
[prompt]$ lein with-profile test profiles
Performing task with-profile with profiles (:dev :user :default)
Performing task 'profiles' with profile(s): 'test'
Performing task profiles with profiles (:test)
debug
default
offline
test
user

Current sticky profiles:
```

Here you can see the verbose statements of slothcfg mixed in with
the statement output by the `with-profile` task. First the
`with-profile` task has its profiles output by slothcfg, before it
runs. Only the default Leiningen profiles are in effect when it
runs. Then it prints out its statement that it is running the
`profiles` task with the `test` profile. Then slothcfg prints out
the profiles in effect when the `profiles` task runs; just the `test`
profile. At the end, you can see that there were no sticky profiles in
effect. If we add the `prod` profile as a sticky profile:

```
[prompt]$ lein set-profile prod
Performing task set-profile with profiles (:dev :user :default)
Current sticky profiles: #{:prod}
[prompt]$ lein with-profile test profiles
Performing task with-profile with profiles (:dev :user :default)
Performing task 'profiles' with profile(s): 'test'
Performing task profiles with profiles (:test :prod)
debug
default
offline
test
user

Current sticky profiles: #{:prod}
```

Here we can see that the `prod` profile was added to the running of
the `profiles` task because it was set sticky, and the `test` profile
was added by the `with-profile` task.

Since all of this extra output is controled by the `:verbose` key in the `:slothcfg` configuration map, you can actually make yourself a profile that has the `:verbose` key to true in a `:slothcfg` map, and then set that profile to by sticky:

```clojure
;; In project.clj...
:profiles {:verbose-slothcfg {:slothcfg {:verbose true}}}
```

Then

```
[prompt]$ lein set-profile verbose-slothcfg
```

will make it so that you can switch slothcfg from verbose output to quiet output by setting or unsetting the `verbose-slothcfg` profile.

```
[prompt]$ lein set-profile verbose-slothcfg
Current sticky profiles: #{:prod :verbose-slothcfg}
[prompt]$ lein jar
Performing task jar with profiles (:dev :user :default :prod :verbose-slothcfg)
Created /path/to/my-great-project/target/my-great-project.jar
```

## Forked version of configleaf<a name="Forked"/>

slothcfg was forked from [Configleaf](https://github.com/davidsantiago/configleaf) because
it appeared to be inactive (it hadn't been updated in over a year). I decided to fork this
project and give it a new name so that folks can include new updates via Leiningen. I decided
to go this route instead of submitting a pull request because the configleaf project has two
outstanding pull requests already (one of them over a year old). The changes in those pull
requests have been incorperated into slothcfg (in fact, slothcfg is actually a fork
of [Justin's pull request](https://github.com/davidsantiago/configleaf/pull/1)).

### Upgrading from configleaf<a name="Upgrading"/>

1. In your project.clj, remove `configleaf.hooks` from `:hooks` and change the configleaf
   entry in your `:plugins` to match what's listed in the [Installation](#Installation) section below.
2. Search your entire project and replace ever occurrence of "configleaf" with "slothcfg".
3. Rename the invisible `.configleaf` directory at the root of your project to `.slothcfg`

## News<a name="News"/>

* Version 1.0.0 (By: [Greg Slepak / @taoeffect](https://github.com/taoeffect))
  * Renamed project to slothcfg so that users can pull new features from Clojars.
  * Removed all use of :use ([it's bad, mmm'k?](http://grokbase.com/t/gg/clojure/137qrc7xmr/can-we-please-deprecate-the-use-directive/nested/))
  * Removed robert-hooke dependency because it comes with leiningen 2.
  * Added :file-ext option to specify config file extension.
  * Added :template option to allow the option of providing your own mustache template.
  * Added :middleware option to optionally transform the project map.
  * Added template for .cljx :file-ext
  * dissoc :checkout-deps-shares from the project map to fix an incompatibility
    with Leiningen 2.3.0 that caused an "Unreadable form" RuntimeException.
  * Included @ninjudd's PR to configleaf to add :keyseq and :var options
  * Now config is stored in an atom to allow for updates within the project.
  * *.clj template uses 'defonce' now.

* Version 0.4.7 (By: [Justin Balthrop / @ninjudd](https://github.com/ninjudd))
  * remove unused v1 legacy methods
  * Add support for outputting a subset of the project map via :keyseq option
  * Added :var option

* Version 0.4.6
  * Implement the `:never-sticky` configuration key.

* Version 0.4.5
  * Bug fixes; remove one of the hooks which is no longer necessary in lein2. Don't use earlier 0.4
    series versions (harm is bounded to extra files being added to JARs or lein tasks failing).

* Version 0.4.3
  * Minor update to also add slothcfg itself to dependencies, fixes similar bugs.

* Version 0.4.2
  * Minor update to automatically add leiningen-core to dependencies, fixes certain
    tasks were hooked but ran in project.

* Version 0.4.1
  * Minor update to ensure that project map metadata is baked along
    with the project map.

* Version 0.4.0
  * Extensive rewrite to work with Leiningen 2.

* Version 0.3.0
  * Renamed and reorganized the project map. Should be easier to explain and use now.

* Version 0.2.0 (By: [David Santiago / @davidsantiago](https://github.com/davidsantiago))
  * Addition of Java system properties to configurations.
  * Changes to configuration map format to allow system properties.

## License<a name="License"/>

Copyright (C) 2011 - 2013

Distributed under the Eclipse Public License, the same as Clojure.

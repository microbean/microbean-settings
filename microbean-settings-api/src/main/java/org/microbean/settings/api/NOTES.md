# Notes

## Proxying

We're in a proxy handler.

If the method being proxied is from Object, do the thing.

If there's a value handler for the method being invoked, get it and
use it, full stop.

Otherwise:

If the method returns:
* A public interface
  * that itself has at least one method
    * that doesn't take parameters and
    * doesn't return `void` or `Void`
    
...then proxy that class and return it.

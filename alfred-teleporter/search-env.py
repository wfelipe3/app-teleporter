import sys
from workflow import Workflow, web


def main(wf):
    query = sys.argv[1]

    params = dict(count=100, format='json')

    res = web.get("http://localhost:8080/env?search=" + query, params)
    res.raise_for_status()

    teleporters = res.json()

    for tep in teleporters:
        type = "teleporter"
        if "teleporters" in tep:
            type = "env"
        wf.add_item(title=tep["name"],
                    valid=True,
                    arg=tep["name"],
                    subtitle=type)

    wf.send_feedback()


if __name__ == "__main__":
    wf = Workflow()
    sys.exit(wf.run(main))
